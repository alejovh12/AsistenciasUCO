package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrearSesionRepositoryMapperTest {

    @Test
    void toRepositoryDTO_con_dominio_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> CrearSesionRepositoryMapper.toRepositoryDTO(null));
    }

    @Test
    void toRepositoryDTO_con_dominio_valido_mapea_campos() {
        final UUID grupo = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final CrearSesionDomain domain = new CrearSesionDomain(
                grupo, "Tema de la sesion", "Descripcion",
                LocalDateTime.of(2026, 1, 20, 8, 0), LocalDateTime.of(2026, 1, 20, 10, 0),
                "Aula 1", "TEORICA", docente, usuarioEjecutor);

        final CrearSesionRepositoryDTO dto = CrearSesionRepositoryMapper.toRepositoryDTO(domain);

        assertEquals(grupo, dto.getGrupo());
        assertEquals("Tema de la sesion", dto.getTema());
        assertEquals(docente, dto.getDocente());
        assertEquals(usuarioEjecutor, dto.getUsuarioEjecutor());
    }
}
