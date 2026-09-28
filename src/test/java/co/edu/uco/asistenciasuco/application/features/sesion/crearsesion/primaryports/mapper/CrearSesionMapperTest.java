package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato TARGET (LB-001B.4A): CrearSesionDTO = (grupo, nombre, fechaHoraInicio, fechaHoraFin,
 * usuarioEjecutor). Sin tema ni docente (Docente.id); el actor es solo Usuario.id.
 * RED esperado: falla la compilacion porque el DTO AS-IS expone tema/docente (6 parametros).
 */
class CrearSesionMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> CrearSesionMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID grupo = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 1, 20, 8, 0);
        final LocalDateTime fin = LocalDateTime.of(2026, 1, 20, 10, 0);
        final CrearSesionDTO dto = new CrearSesionDTO(grupo, "Nombre de la sesion", inicio, fin, usuarioEjecutor);

        final CrearSesionDomain domain = CrearSesionMapper.toDomain(dto);

        assertEquals(grupo, domain.getGrupo());
        assertEquals("Nombre de la sesion", domain.getNombre());
        assertEquals(inicio, domain.getFechaHoraInicio());
        assertEquals(fin, domain.getFechaHoraFin());
        assertEquals(usuarioEjecutor, domain.getUsuarioEjecutor());
    }
}
