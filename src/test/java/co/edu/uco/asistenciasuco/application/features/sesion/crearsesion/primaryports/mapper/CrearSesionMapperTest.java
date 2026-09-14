package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrearSesionMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> CrearSesionMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID grupo = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final CrearSesionDTO dto = new CrearSesionDTO(
                grupo, "Tema de la sesion", "Descripcion",
                LocalDateTime.of(2026, 1, 20, 8, 0), LocalDateTime.of(2026, 1, 20, 10, 0),
                "Aula 1", "TEORICA", docente);

        final CrearSesionDomain domain = CrearSesionMapper.toDomain(dto);

        assertEquals(grupo, domain.getGrupo());
        assertEquals("Tema de la sesion", domain.getTema());
        assertEquals(docente, domain.getDocente());
    }
}
