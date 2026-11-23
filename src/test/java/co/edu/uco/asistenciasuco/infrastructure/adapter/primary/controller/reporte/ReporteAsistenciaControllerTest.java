package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.reporte;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaExcelInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.GenerarReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El contenido del libro se verifica en {@code PoiReporteExcelAdapterTest}; aqui solo el contrato HTTP del controlador. */
class ReporteAsistenciaControllerTest {

    @Test
    void entrega_el_libro_del_puerto_con_su_tipo_nombre_y_el_ejecutor_autenticado() {
        final UUID grupoId = UUID.randomUUID();
        final UUID actorId = UUID.randomUUID();
        final byte[] libro = {1, 2, 3};
        final AtomicReference<GenerarReporteAsistenciaDTO> request = new AtomicReference<>();
        final GenerarReporteAsistenciaExcelInputPort port = input -> {
            request.set(input);
            return libro;
        };
        final AuthenticatedUserResolver resolver = () -> actorId;

        final ResponseEntity<byte[]> response = new ReporteAsistenciaController(port, resolver).generarReporteExcel(grupoId);

        assertEquals(grupoId, request.get().grupoId());
        assertEquals(actorId, request.get().usuarioEjecutor());
        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getContentDisposition().getFilename().contains(grupoId.toString()));
        assertArrayEquals(libro, response.getBody());
    }

    /** CULM-001 D.9-a (F-TRX-021): grupo inexistente o fuera de ambito responde 403 y no se genera ningun .xlsx. */
    @Test
    void culm001_d9_grupo_inexistente_o_ajeno_responde_403_sin_archivo() throws Exception {
        final GenerarReporteAsistenciaExcelInputPort port = input -> {
            throw new co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException("SEC_002", "Sin autorizacion.");
        };
        final MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new ReporteAsistenciaController(port, UUID::randomUUID))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
                .build();

        mvc.perform(get("/api/v1/grupos/{id}/reportes/asistencia-excel", UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }
}
