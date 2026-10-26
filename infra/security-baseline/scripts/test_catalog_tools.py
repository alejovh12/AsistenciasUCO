"""Pruebas offline (stdlib) del descubrimiento de endpoints y del catálogo de acceso.

Uso: python -X utf8 -I -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"

No requieren Docker, lab.env ni red: cubren las variantes de mapeo que `extract_endpoints.py` debe entender, la validación
estricta de `access-catalog.json`, la deriva catálogo↔PDP y que el catálogo vigente coincide con los controladores.
Complementan a `AccessCatalogConsistencyTest` (Spring MVC real y SecurityFilterChain real).
"""
import copy
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault("USERPROFILE", tempfile.gettempdir())  # lab_pdp calcula rutas al importarse; no lee archivos
import extract_endpoints as X  # noqa: E402
import provision_asistencias as P  # noqa: E402


def endpoints_of(java_source):
    with tempfile.TemporaryDirectory() as folder:
        path = os.path.join(folder, "SampleController.java")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(java_source)
        return X.file_endpoints(path)


class ExtractEndpointsTest(unittest.TestCase):
    def test_class_prefix_and_basic_verbs(self):
        found = endpoints_of('''
            @RestController @RequestMapping("/api/v1/demo")
            public final class SampleController {
                @GetMapping public void a() {}
                @PostMapping("/items") public void b() {}
                @PutMapping(path = "/items/{id}") public void c() {}
                @PatchMapping(value = "/items/{id}/estado") public void d() {}
                @DeleteMapping("/items/{id}") public void e() {}
            }''')
        self.assertEqual(found, {
            ("GET", "/api/v1/demo"), ("POST", "/api/v1/demo/items"), ("PUT", "/api/v1/demo/items/{id}"),
            ("PATCH", "/api/v1/demo/items/{id}/estado"), ("DELETE", "/api/v1/demo/items/{id}")})

    def test_multiple_paths_and_multiple_methods(self):
        found = endpoints_of('''
            @RestController @RequestMapping({"/api/v1/a", "/api/v1/b"})
            class SampleController {
                @GetMapping({"/x", "/y/{id}"}) void a() {}
                @RequestMapping(value = "/z", method = {RequestMethod.POST, RequestMethod.PUT}) void b() {}
                @RequestMapping(path = "/w", method = RequestMethod.DELETE) void c() {}
            }''')
        for prefix in ("/api/v1/a", "/api/v1/b"):
            self.assertIn(("GET", prefix + "/x"), found)
            self.assertIn(("GET", prefix + "/y/{id}"), found)
            self.assertIn(("POST", prefix + "/z"), found)
            self.assertIn(("PUT", prefix + "/z"), found)
            self.assertIn(("DELETE", prefix + "/w"), found)
        self.assertEqual(len(found), 10)

    def test_regex_variables_are_normalized_and_comments_are_ignored(self):
        found = endpoints_of('''
            // @GetMapping("/comentado")
            /* class Falsa { @PostMapping("/tambien-comentado") } */
            @RestController
            @RequestMapping("/api/v1/demo")
            public class SampleController {
                /** @DeleteMapping("/javadoc") */
                @GetMapping("/items/{id:[0-9]+}") void a() {}
                @GetMapping(value = "/con-parentesis", produces = MediaType.valueOf("text/plain")) void b() {}
                String url = "http://ejemplo//no-es-comentario";
            }''')
        self.assertEqual(found, {("GET", "/api/v1/demo/items/{id}"), ("GET", "/api/v1/demo/con-parentesis")})

    def test_non_controllers_are_ignored(self):
        self.assertEqual(endpoints_of('@Service class X { @GetMapping("/no") void a() {} }'), set())

    def test_non_literal_paths_and_missing_verbs_fail_loudly(self):
        with self.assertRaises(X.ExtractionError):
            endpoints_of('@RestController @RequestMapping("/api/v1/d") class S { @GetMapping(Paths.ITEMS) void a() {} }')
        with self.assertRaises(X.ExtractionError):
            endpoints_of('@RestController @RequestMapping("/api/v1/d") class S { @GetMapping("/a" + SUFFIX) void a() {} }')
        with self.assertRaises(X.ExtractionError):
            endpoints_of('@RestController @RequestMapping("/api/v1/d") class S { @RequestMapping("/a") void a() {} }')

    def test_current_sources_are_extracted_without_errors(self):
        found = X.endpoints()
        self.assertGreater(len(found), 100)
        self.assertEqual(len(found), len(set(found)))
        self.assertTrue(all(path.startswith("/api/") and "*" not in path for _, path in found))


class CatalogValidationTest(unittest.TestCase):
    def setUp(self):
        self.catalog = P.load_catalog()

    def mutate(self, change):
        catalog = copy.deepcopy(self.catalog)
        change(catalog)
        return P.validate_catalog(catalog)

    def test_current_catalog_is_valid_and_complete(self):
        self.assertEqual(P.validate_catalog(self.catalog), [])
        missing, stale = P.inventory_gaps(self.catalog)
        self.assertEqual((missing, stale), ([], []), "endpoints sin regla / reglas obsoletas en access-catalog.json")

    def test_every_profile_is_non_empty_and_every_role_belongs_to_a_profile(self):
        covered = set()
        for profile in self.catalog["profiles"]:
            self.assertTrue(profile["roles"], profile["name"])
            covered |= set(profile["roles"])
        self.assertEqual(covered, set(P.ROLES))

    def test_duplicates_wildcards_and_foreign_roles_are_rejected(self):
        self.assertTrue(self.mutate(lambda c: c["endpoints"].append(dict(c["endpoints"][0]))))
        self.assertTrue(self.mutate(lambda c: c["endpoints"][0].update(path="/api/v1/**")))
        self.assertTrue(self.mutate(lambda c: c["endpoints"][0].update(roles=["SUPERUSUARIO"])))
        self.assertTrue(self.mutate(lambda c: c["endpoints"][0].update(roles=[])))
        self.assertTrue(self.mutate(lambda c: c["endpoints"][0].update(method="TRACE")))

    def test_granting_all_five_roles_requires_authenticated_access(self):
        row = next(e for e in self.catalog["endpoints"] if len(e["roles"]) < 5)
        self.assertTrue(self.mutate(lambda c: c["endpoints"][c["endpoints"].index(
            next(x for x in c["endpoints"] if x["path"] == row["path"] and x["method"] == row["method"]))]
            .update(roles=list(P.ROLES), access="ROLES")))
        authenticated = next(e for e in self.catalog["endpoints"] if e["access"] == "AUTHENTICATED")
        self.assertEqual(len(authenticated["roles"]), 5)

    def test_empty_or_unknown_profiles_are_rejected(self):
        self.assertTrue(self.mutate(lambda c: c["profiles"].append({"name": "Vacío", "roles": []})))
        self.assertTrue(self.mutate(lambda c: c["profiles"].append({"name": "Ajeno", "roles": ["OTRO"]})))
        self.assertTrue(self.mutate(lambda c: c["profiles"].append(dict(c["profiles"][0]))))

    def test_unknown_endpoint_in_code_is_reported_as_missing_rule(self):
        catalog = copy.deepcopy(self.catalog)
        catalog["endpoints"] = [e for e in catalog["endpoints"] if e["path"] != "/api/v1/auth/me"]
        missing, stale = P.inventory_gaps(catalog)
        self.assertEqual(missing, [("GET", "/api/v1/auth/me")])
        self.assertEqual(stale, [])


class DriftTest(unittest.TestCase):
    def state(self, catalog):
        want = P.desired_grants(catalog)
        return {
            "resources": {pair: "id" for pair in P.catalog_pairs(catalog)},
            "roles": {r: "rid" for r in P.ROLES},
            "grants": {r: set(want[r]) for r in P.ROLES},
            "profiles": {p["name"]: "pid" for p in catalog["profiles"]},
            "profile_roles": {p["name"]: set(p["roles"]) for p in catalog["profiles"]},
        }

    def test_identical_state_is_clean(self):
        catalog = P.load_catalog()
        self.assertTrue(P.is_clean(P.diff_state(catalog, self.state(catalog))))

    def test_missing_extra_and_orphan_are_reported_separately(self):
        catalog = P.load_catalog()
        state = self.state(catalog)
        target = ("GET", "/api/v1/auth/me")
        state["grants"]["ESTUDIANTE"].discard(target)
        state["grants"]["ESTUDIANTE"].add(("POST", "/api/v1/grupos"))  # concesión que el catálogo no autoriza
        state["resources"][("POST", "/api/v1/ruta-retirada")] = "id"
        diff = P.diff_state(catalog, state)
        self.assertEqual(diff["missing_grants"]["ESTUDIANTE"], [target])
        self.assertEqual(diff["extra_grants"]["ESTUDIANTE"], [("POST", "/api/v1/grupos")])
        self.assertEqual(diff["orphan_resources"], [("POST", "/api/v1/ruta-retirada")])
        self.assertFalse(P.is_clean(diff))

    def test_orphan_resource_alone_does_not_open_access_and_is_not_a_failure(self):
        catalog = P.load_catalog()
        state = self.state(catalog)
        state["resources"][("POST", "/api/v1/ruta-retirada")] = "id"
        self.assertTrue(P.is_clean(P.diff_state(catalog, state)))

    def test_profile_with_wrong_roles_is_reported(self):
        catalog = P.load_catalog()
        state = self.state(catalog)
        state["profile_roles"]["Docencia"] = {"DOCENTE", "ADMINISTRADOR"}
        diff = P.diff_state(catalog, state)
        self.assertEqual(diff["profile_role_gaps"], {"Docencia": ["ADMINISTRADOR"]})
        self.assertFalse(P.is_clean(diff))


if __name__ == "__main__":
    unittest.main()
