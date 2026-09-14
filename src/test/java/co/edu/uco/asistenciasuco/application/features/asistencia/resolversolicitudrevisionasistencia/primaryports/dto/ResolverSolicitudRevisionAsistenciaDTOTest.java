package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResolverSolicitudRevisionAsistenciaDTOTest {

    @Test
    void constructor_completo_asigna_todos_los_campos() {
        final UUID solicitud = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();

        final ResolverSolicitudRevisionAsistenciaDTO dto =
                new ResolverSolicitudRevisionAsistenciaDTO(solicitud, "APROBAR", "Aceptada", usuario);

        assertEquals(solicitud, dto.getSolicitud());
        assertEquals("APROBAR", dto.getAccion());
        assertEquals("Aceptada", dto.getRespuestaDocente());
        assertEquals(usuario, dto.getUsuario());
    }

    @Test
    void constructor_vacio_y_setters_permiten_construccion_incremental() {
        final ResolverSolicitudRevisionAsistenciaDTO dto = new ResolverSolicitudRevisionAsistenciaDTO();

        dto.setSolicitud(null);
        dto.setAccion("RECHAZAR");
        dto.setRespuestaDocente(null);
        dto.setUsuario(null);

        assertNull(dto.getSolicitud());
        assertEquals("RECHAZAR", dto.getAccion());
        assertNull(dto.getRespuestaDocente());
        assertNull(dto.getUsuario());
    }
}
