"""Pure stdlib unit tests; no SQL Server, Docker or network dependencies."""

import io
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from verify_jdbc_artifact import PATCHED_ARTIFACT, PATCHED_LIBRARY, POM_PROPERTIES, attest


def archive(entries: list[tuple[str, str | None]], *, metadata: str | None = None):
    outer_buf = io.BytesIO()
    with ZipFile(outer_buf, "w") as outer:
        for name, content in entries:
            if content is None:
                jar_buf = io.BytesIO()
                with ZipFile(jar_buf, "w") as jar:
                    jar.writestr(POM_PROPERTIES, metadata or (
                        "groupId=com.microsoft.sqlserver\nartifactId=mssql-jdbc\nversion=13.2.1\n"
                    ))
                outer.writestr(name, jar_buf.getvalue())
            else:
                outer.writestr(name, content)
    return outer_buf.getvalue()


class JdbcArtifactTests(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.path = Path(self.folder.name) / "app.jar"

    def write(self, entries, metadata=None):
        self.path.write_bytes(archive(entries, metadata=metadata))
        return self.path

    def test_patched_artifact_is_attested(self):
        self.assertIn("verified", attest(self.write([(PATCHED_ARTIFACT, None)])))

    def test_old_driver_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Expected only"):
            attest(self.write([("BOOT-INF/lib/mssql-jdbc-13.2.0.jre11.jar", None)]))

    def test_missing_driver_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Expected only"):
            attest(self.write([("BOOT-INF/lib/other.jar", "placeholder")]))

    def test_conflicting_jdbc_artifacts_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "Expected only"):
            attest(self.write([(PATCHED_ARTIFACT, None), ("BOOT-INF/lib/mssql-jdbc-12.8.1.jre11.jar", None)]))

    def test_wrong_metadata_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unexpected JDBC driver groupId"):
            attest(self.write([(PATCHED_ARTIFACT, None)], metadata=(
                "groupId=example.invalid\nartifactId=mssql-jdbc\nversion=13.2.1\n"
            )))

    def test_bad_nested_jar_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unable to inspect"):
            attest(self.write([(PATCHED_ARTIFACT, "not-a-jar")]))

    def test_wrong_inner_version_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unexpected JDBC Maven version"):
            attest(self.write([(PATCHED_ARTIFACT, None)], metadata=(
                "groupId=com.microsoft.sqlserver\nartifactId=mssql-jdbc\nversion=13.2.0\n"
            )))


def driver_jar(metadata: str | None = None) -> bytes:
    buf = io.BytesIO()
    with ZipFile(buf, "w") as jar:
        jar.writestr(POM_PROPERTIES, metadata or "groupId=com.microsoft.sqlserver\nartifactId=mssql-jdbc\nversion=13.2.1\n")
    return buf.getvalue()


class LayeredRuntimeTests(unittest.TestCase):
    """Layered image: /app holds the thin app.jar and the libraries extracted to lib/."""

    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.app = Path(self.folder.name)
        (self.app / "lib").mkdir()

    def put(self, relative: str, content: bytes) -> None:
        target = self.app / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)

    def test_patched_library_is_attested(self):
        self.put(PATCHED_LIBRARY, driver_jar())
        self.assertIn("verified", attest(self.app))

    def test_old_library_is_rejected(self):
        self.put("lib/mssql-jdbc-13.2.0.jre11.jar", driver_jar())
        with self.assertRaisesRegex(ValueError, "Expected only"):
            attest(self.app)

    def test_missing_library_is_rejected(self):
        self.put("lib/other.jar", b"placeholder")
        with self.assertRaisesRegex(ValueError, "Expected only"):
            attest(self.app)

    def test_second_driver_anywhere_is_rejected(self):
        self.put(PATCHED_LIBRARY, driver_jar())
        self.put("cache/mssql-jdbc-12.8.1.jre11.jar", driver_jar())
        with self.assertRaisesRegex(ValueError, "Expected only"):
            attest(self.app)

    def test_wrong_metadata_is_rejected(self):
        self.put(PATCHED_LIBRARY, driver_jar("groupId=example.invalid\nartifactId=mssql-jdbc\nversion=13.2.1\n"))
        with self.assertRaisesRegex(ValueError, "Unexpected JDBC driver groupId"):
            attest(self.app)

    def test_corrupt_library_is_rejected(self):
        self.put(PATCHED_LIBRARY, b"not a jar")
        with self.assertRaisesRegex(ValueError, "Unable to inspect"):
            attest(self.app)


if __name__ == "__main__":
    unittest.main()
