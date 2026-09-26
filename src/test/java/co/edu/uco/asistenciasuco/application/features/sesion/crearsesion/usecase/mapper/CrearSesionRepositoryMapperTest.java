package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato TARGET (LB-001B.4A): CrearSesionRepositoryDTO(grupo, nombre, fechaHoraInicio,
 * fechaHoraFin, usuarioEjecutor). Sin tema ni docente.
 * RED esperado: falla la compilacion (constructor de 5 parametros / getNombre inexistentes).
 */
class CrearSesionRepositoryMapperTest {

    @Test
    void toRepositoryDTO_con_dominio_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> CrearSesionRepositoryMapper.toRepositoryDTO(null));
    }

    @Test
    void toRepositoryDTO_con_dominio_valido_mapea_campos() {
        final UUID grupo = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 1, 20, 8, 0);
        final LocalDateTime fin = LocalDateTime.of(2026, 1, 20, 10, 0);
        final CrearSesionDomain domain = new CrearSesionDomain(grupo, "Nombre de la sesion", inicio, fin, usuarioEjecutor);

        final CrearSesionRepositoryDTO dto = CrearSesionRepositoryMapper.toRepositoryDTO(domain);

        assertEquals(grupo, dto.getGrupo());
        assertEquals("Nombre de la sesion", dto.getNombre());
        assertEquals(inicio, dto.getFechaHoraInicio());
        assertEquals(fin, dto.getFechaHoraFin());
        assertEquals(usuarioEjecutor, dto.getUsuarioEjecutor());
    }
}
