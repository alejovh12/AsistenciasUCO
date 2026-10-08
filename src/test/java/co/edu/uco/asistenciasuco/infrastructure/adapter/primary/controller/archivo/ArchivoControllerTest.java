package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.SubirArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoResultado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RED causal LB-004B.2 STORAGE-001: el controller depende exclusivamente de InputPorts; no
 * conoce MinIO, ClamAV ni filesystem. Cobertura HTTP-shape del contrato
 * docs/work-items/LB-004-stateless-serverless-readiness/HTTP_CONTRACT_TARGET.md.
 */
class ArchivoControllerTest {

    private final SubirArchivoInputPort subirArchivoInputPort = mock(SubirArchivoInputPort.class);
    private final DescargarArchivoInputPort descargarArchivoInputPort = mock(DescargarArchivoInputPort.class);
    private final AuthenticatedUserResolver authenticatedUserResolver = mock(AuthenticatedUserResolver.class);
    private ArchivoController controller;
    private UUID authenticatedUserId;

    @BeforeEach
    void setUp() {
        authenticatedUserId = UUID.randomUUID();
        when(authenticatedUserResolver.requireAuthenticatedUserId()).thenReturn(authenticatedUserId);
        controller = new ArchivoController(subirArchivoInputPort, descargarArchivoInputPort, authenticatedUserResolver);
    }

    @Test
    void subida_exitosa_retorna_201_con_fileId_nombre_url_y_tamanio() {
        final UUID fileId = UUID.randomUUID();
        when(subirArchivoInputPort.execute(any())).thenReturn(
                new SubirArchivoResultado(fileId, "soporte.pdf", "/api/v1/archivos/" + fileId, 13L));

        final ResponseEntity<ApiDataResponse<Map<String, Object>>> response = controller.subirArchivo(
                new MockMultipartFile("archivo", "soporte.pdf", "application/pdf", "contenido pdf".getBytes()));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final Map<String, Object> data = response.getBody().datos();
        assertEquals(fileId, data.get("fileId"));
        assertEquals("soporte.pdf", data.get("nombre"));
        assertEquals("/api/v1/archivos/" + fileId, data.get("url"));
        assertEquals(13L, data.get("tamanio"));
    }

    @Test
    void subida_usa_la_identidad_autenticada_como_owner_nunca_un_campo_de_request() {
        when(subirArchivoInputPort.execute(any())).thenReturn(
                new SubirArchivoResultado(UUID.randomUUID(), "x.pdf", "/api/v1/archivos/x", 1L));

        controller.subirArchivo(new MockMultipartFile("archivo", "x.pdf", "application/pdf", "c".getBytes()));

        final var captor = org.mockito.ArgumentCaptor.forClass(SubirArchivoDTO.class);
        verify(subirArchivoInputPort).execute(captor.capture());
        assertEquals(authenticatedUserId, captor.getValue().ownerSubject());
        assertEquals("application/pdf", captor.getValue().declaredContentType());
    }

    @Test
    void descarga_exitosa_retorna_200_con_bytes_y_content_type() {
        final UUID fileId = UUID.randomUUID();
        final byte[] content = "contenido pdf".getBytes();
        when(descargarArchivoInputPort.execute(any()))
                .thenReturn(new DescargarArchivoResultado(content, "application/pdf", "soporte.pdf", content.length));

        final ResponseEntity<byte[]> response = controller.descargarArchivo(fileId.toString());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertArrayEquals(content, response.getBody());
    }

    @Test
    void descarga_usa_el_requester_autenticado_y_el_fileId_del_path() {
        final UUID fileId = UUID.randomUUID();
        when(descargarArchivoInputPort.execute(any()))
                .thenReturn(new DescargarArchivoResultado("c".getBytes(), "application/pdf", "x.pdf", 1));

        controller.descargarArchivo(fileId.toString());

        final var captor = org.mockito.ArgumentCaptor.forClass(DescargarArchivoDTO.class);
        verify(descargarArchivoInputPort).execute(captor.capture());
        assertEquals(fileId, captor.getValue().fileId());
        assertEquals(authenticatedUserId, captor.getValue().requesterSubject());
    }

    // fileId malformado: 404, nunca 400 — no distingue "no existe" de "formato invalido".
    @Test
    void fileId_malformado_en_descarga_retorna_resource_not_found() {
        assertThrows(ResourceNotFoundException.class, () -> controller.descargarArchivo("no-es-un-uuid"));
    }

    @Test
    void ownership_denegado_propaga_resource_not_found_del_use_case() {
        when(descargarArchivoInputPort.execute(any())).thenThrow(new ResourceNotFoundException("El archivo solicitado no existe."));

        assertThrows(ResourceNotFoundException.class, () -> controller.descargarArchivo(UUID.randomUUID().toString()));
    }
}
