package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain.ActualizarSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato TARGET (LB-001B.4A): ActualizarSesionDTO(sesion, nombre, fechaHoraInicio, fechaHoraFin,
 * usuarioEjecutor). Sin docente. RED esperado: falla la compilacion (constructor AS-IS de 6 parametros).
 */
class ActualizarSesionMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ActualizarSesionMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID sesion = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 1, 20, 8, 0);
        final LocalDateTime fin = LocalDateTime.of(2026, 1, 20, 10, 0);
        final ActualizarSesionDTO dto = new ActualizarSesionDTO(sesion, "Sesion actualizada", inicio, fin, usuarioEjecutor);

        final ActualizarSesionDomain domain = ActualizarSesionMapper.toDomain(dto);

        assertEquals(sesion, domain.getSesion());
        assertEquals("Sesion actualizada", domain.getNombre());
        assertEquals(inicio, domain.getFechaHoraInicio());
        assertEquals(fin, domain.getFechaHoraFin());
        assertEquals(usuarioEjecutor, domain.getUsuarioEjecutor());
    }
}
