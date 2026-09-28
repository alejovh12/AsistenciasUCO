package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain.ActualizarSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato TARGET (LB-001B.4A): ActualizarSesionRepositoryDTO(sesion, nombre, fechaHoraInicio,
 * fechaHoraFin, usuarioEjecutor). Sin docente. RED esperado: falla la compilacion (record de 6 componentes).
 */
class ActualizarSesionRepositoryMapperTest {

    @Test
    void toRepositoryDTO_con_dominio_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ActualizarSesionRepositoryMapper.toRepositoryDTO(null));
    }

    @Test
    void toRepositoryDTO_con_dominio_valido_mapea_campos() {
        final UUID sesion = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 1, 20, 8, 0);
        final LocalDateTime fin = LocalDateTime.of(2026, 1, 20, 10, 0);
        final ActualizarSesionDomain domain = new ActualizarSesionDomain(sesion, "Sesion actualizada", inicio, fin, usuarioEjecutor);

        final ActualizarSesionRepositoryDTO dto = ActualizarSesionRepositoryMapper.toRepositoryDTO(domain);

        assertEquals(sesion, dto.sesion());
        assertEquals("Sesion actualizada", dto.nombre());
        assertEquals(inicio, dto.fechaHoraInicio());
        assertEquals(fin, dto.fechaHoraFin());
        assertEquals(usuarioEjecutor, dto.usuarioEjecutor());
    }
}
