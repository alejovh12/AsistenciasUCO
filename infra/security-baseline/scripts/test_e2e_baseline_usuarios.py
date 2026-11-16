"""Pruebas offline (stdlib) de la elección de usuarios del E2E de cinco roles (ROL-12, VAL-oleada8).

Uso: python -X utf8 -I -m unittest discover -s infra/security-baseline/scripts -p "test_*.py"
No llaman a Keycloak, al backend ni leen lab.env.
"""
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault("USERPROFILE", tempfile.gettempdir())  # lab_pdp calcula rutas al importarse; no lee archivos
import e2e_baseline as E2E  # noqa: E402


class UsuariosPorRolTest(unittest.TestCase):

    def test_sin_variables_usa_las_cuentas_uco_de_cada_rol_en_orden(self):
        self.assertEqual(
            [("ADMINISTRADOR", "uco-administrador"), ("DECANO", "uco-decano"), ("COORDINADOR", "uco-coordinador"),
             ("DOCENTE", "uco-docente"), ("ESTUDIANTE", "uco-estudiante")],
            E2E.usuarios_por_rol({}))

    def test_cada_rol_se_puede_cambiar_por_su_variable(self):
        usuarios = dict(E2E.usuarios_por_rol({
            "E2E_USUARIO_DOCENTE": "maria.rostagno@uco.edu.co",
            "E2E_USUARIO_ESTUDIANTE": "carlos.zapata@uco.edu.co",
        }))
        self.assertEqual("maria.rostagno@uco.edu.co", usuarios["DOCENTE"])
        self.assertEqual("carlos.zapata@uco.edu.co", usuarios["ESTUDIANTE"])
        self.assertEqual("uco-decano", usuarios["DECANO"])

    def test_una_variable_vacia_no_deja_el_rol_sin_usuario(self):
        self.assertEqual("uco-coordinador", dict(E2E.usuarios_por_rol({"E2E_USUARIO_COORDINADOR": ""}))["COORDINADOR"])


if __name__ == "__main__":
    unittest.main()
