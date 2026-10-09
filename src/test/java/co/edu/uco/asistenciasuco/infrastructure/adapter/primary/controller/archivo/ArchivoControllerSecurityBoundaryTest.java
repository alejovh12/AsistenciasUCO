package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.SubirArchivoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QUALITY-PR15 F08/F09: authorization boundaries and response header sanitation.
 * Direct controller tests are NOT JWT/filter-chain tests and do NOT establish E2E.
 */
class ArchivoControllerSecurityBoundaryTest {
    private final SubirArchivoInputPort upload = mock(SubirArchivoInputPort.class);
    private final DescargarArchivoInputPort download = mock(DescargarArchivoInputPort.class);
    private final AuthenticatedUserResolver identity = mock(AuthenticatedUserResolver.class);
    private final ArchivoController controller = new ArchivoController(upload, download, identity);

    @Test
    void descarga_sin_identidad_no_consulta_el_archivo() {
        when(identity.requireAuthenticatedUserId()).thenThrow(new IllegalStateException("no identity"));
        assertThrows(IllegalStateException.class,
                () -> controller.descargarArchivo(UUID.randomUUID().toString()));
        verifyNoInteractions(download, upload);
    }

    @Test
    void subida_sin_identidad_no_invoca_el_caso_de_uso() {
        when(identity.requireAuthenticatedUserId()).thenThrow(new IllegalStateException("no identity"));
        assertThrows(IllegalStateException.class, () -> controller.subirArchivo(
                new MockMultipartFile("archivo", "safe.pdf", "application/pdf", "%PDF-1.4".getBytes())));
        verifyNoInteractions(upload, download);
    }

    @Test
    void path_con_fileid_malformado_no_invoca_descarga() {
        when(identity.requireAuthenticatedUserId()).thenReturn(UUID.randomUUID());
        assertThrows(ResourceNotFoundException.class, () -> controller.descargarArchivo("../../../etc/passwd"));
        verifyNoInteractions(download);
    }

    @Test
    void filename_con_comillas_y_saltos_de_linea_no_inyecta_cabeceras() {
        when(identity.requireAuthenticatedUserId()).thenReturn(UUID.randomUUID());
        when(download.execute(any())).thenReturn(new DescargarArchivoResultado(
                "%PDF-".getBytes(), "application/pdf", "reporte\"\r\nX-Injected: 1.pdf", 5L));
        final ResponseEntity<byte[]> response = controller.descargarArchivo(UUID.randomUUID().toString());
        final String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertFalse(disposition.contains("\r"));
        assertFalse(disposition.contains("\n"));
        assertFalse(disposition.contains("reporte\""));
        assertTrue(disposition.contains("filename=\""));
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
    }
}
