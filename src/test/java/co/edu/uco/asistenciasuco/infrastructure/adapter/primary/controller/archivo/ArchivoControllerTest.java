package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivoControllerTest {

    @TempDir
    Path storageDirectory;

    private ArchivoController controller;

    @BeforeEach
    void setUp() {
        controller = new ArchivoController(storageDirectory.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../archivo.pdf", "..\\archivo.pdf", "/tmp/archivo.pdf", "C:\\archivo.pdf", "a..b.pdf"})
    void rechazaRutasEnNombreDeSubida(final String nombre) {
        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.subirArchivo(file(nombre, "pdf")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../archivo.pdf", "..\\archivo.pdf", "/tmp/archivo.pdf", "C:\\archivo.pdf", "a..b.pdf"})
    void rechazaRutasEnNombreDeDescarga(final String nombre) {
        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.descargarArchivo(nombre));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void rechazaRutaAbsolutaConstruidaDesdeElDirectorioLocal() {
        final String nombre = storageDirectory.resolve("archivo.pdf").toString();

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> controller.subirArchivo(file(nombre, "pdf"))).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> controller.descargarArchivo(nombre)).getStatusCode());
    }

    @Test
    void subeYDescargaArchivoValidoDentroDelDirectorio() throws IOException {
        final byte[] content = "%PDF-1.4\ncontenido".getBytes();
        final ResponseEntity<ApiDataResponse<Map<String, Object>>> upload = controller.subirArchivo(
                new MockMultipartFile("archivo", "soporte medico.pdf", "application/pdf", content));
        final Map<String, Object> data = upload.getBody().datos();
        final String savedName = (String) data.get("nombreGuardado");

        assertEquals(HttpStatus.CREATED, upload.getStatusCode());
        assertEquals("soporte medico.pdf", data.get("nombre"));
        assertEquals("/api/v1/archivos/" + savedName, data.get("url"));
        assertTrue(savedName.endsWith("_soporte_medico.pdf"));
        assertArrayEquals(content, Files.readAllBytes(storageDirectory.resolve(savedName)));

        final ResponseEntity<Resource> download = controller.descargarArchivo(savedName);
        assertEquals(HttpStatus.OK, download.getStatusCode());
        assertEquals("application/pdf", download.getHeaders().getContentType().toString());
        try (var input = download.getBody().getInputStream()) {
            assertArrayEquals(content, input.readAllBytes());
        }
    }

    @Test
    void archivoInexistenteRetorna404() {
        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.descargarArchivo("no-existe.pdf"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void extensionNoPermitidaRetorna400SinCrearArchivo() throws IOException {
        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.subirArchivo(file("script.exe", "exe")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        try (var files = Files.list(storageDirectory)) {
            assertFalse(files.findAny().isPresent());
        }
    }

    @Test
    void archivoVacioRetorna400() {
        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.subirArchivo(new MockMultipartFile("archivo", "soporte.pdf", "application/pdf", new byte[0])));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void archivoMayorDeCincoMegabytesRetorna400() {
        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.subirArchivo(new MockMultipartFile("archivo", "soporte.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1])));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void subeYDescargaImagenPngConTipoDeContenidoCorrecto() throws IOException {
        final byte[] content = "imagen".getBytes();
        final ResponseEntity<ApiDataResponse<Map<String, Object>>> upload = controller.subirArchivo(
                new MockMultipartFile("archivo", "captura.png", "image/png", content));
        final String savedName = (String) upload.getBody().datos().get("nombreGuardado");

        final ResponseEntity<Resource> download = controller.descargarArchivo(savedName);

        assertEquals("image/png", download.getHeaders().getContentType().toString());
    }

    @Test
    void subeYDescargaImagenJpgConTipoDeContenidoCorrecto() throws IOException {
        final byte[] content = "imagen".getBytes();
        final ResponseEntity<ApiDataResponse<Map<String, Object>>> upload = controller.subirArchivo(
                new MockMultipartFile("archivo", "captura.jpg", "image/jpeg", content));
        final String savedName = (String) upload.getBody().datos().get("nombreGuardado");

        final ResponseEntity<Resource> download = controller.descargarArchivo(savedName);

        assertEquals("image/jpeg", download.getHeaders().getContentType().toString());
    }

    @Test
    void nombreDeArchivoSinExtensionUsaOctetStreamPorDefecto() throws IOException {
        final Path fileWithoutExtension = storageDirectory.resolve("sinextension");
        Files.write(fileWithoutExtension, "contenido".getBytes());

        final ResponseEntity<Resource> download = controller.descargarArchivo("sinextension");

        assertEquals("application/octet-stream", download.getHeaders().getContentType().toString());
    }

    @Test
    void nombreNuloOEnBlancoEsRechazadoEnDescarga() {
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> controller.descargarArchivo("   ")).getStatusCode());
    }

    @Test
    void archivoConNombreOriginalNuloUsaValorPorDefectoYRechazaPorSinExtension() {
        final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.subirArchivo(new MockMultipartFile("archivo", null, "application/pdf", "contenido".getBytes())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private MockMultipartFile file(final String name, final String extension) {
        return new MockMultipartFile("archivo", name, "application/" + extension, "contenido".getBytes());
    }
}
