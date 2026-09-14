package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain.ActualizarSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActualizarSesionMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ActualizarSesionMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID sesion = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final ActualizarSesionDTO dto = new ActualizarSesionDTO(
                sesion, "Sesion actualizada", LocalDateTime.of(2026, 1, 20, 8, 0),
                LocalDateTime.of(2026, 1, 20, 10, 0), "Aula 1", "Descripcion", docente);

        final ActualizarSesionDomain domain = ActualizarSesionMapper.toDomain(dto);

        assertEquals(sesion, domain.getSesion());
        assertEquals("Sesion actualizada", domain.getNombre());
        assertEquals(docente, domain.getDocente());
    }
}
