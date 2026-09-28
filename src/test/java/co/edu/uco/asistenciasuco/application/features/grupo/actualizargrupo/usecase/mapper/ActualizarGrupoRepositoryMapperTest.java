package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.entity.ActualizarGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActualizarGrupoRepositoryMapperTest {

    @Test
    void toRepositoryDTO_mapea_todos_los_campos() {
        final UUID grupo = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final ActualizarGrupoDomain domain = new ActualizarGrupoDomain(
                grupo, 1, "Grupo 1", UUID.randomUUID(), 30, usuarioEjecutor
        );

        final ActualizarGrupoRepositoryDTO dto = ActualizarGrupoRepositoryMapper.toRepositoryDTO(domain);

        assertEquals(grupo, dto.idGrupo());
        assertEquals(30, dto.cupoMaximo());
        assertEquals(usuarioEjecutor, dto.usuarioEjecutor());
    }

    @Test
    void toEntity_mapea_proyeccion_de_comando() {
        final UUID grupo = UUID.randomUUID();
        final GrupoCommandRepositoryProjection projection = new GrupoCommandRepositoryProjection(grupo, "Grupo actualizado.");

        final ActualizarGrupoResultadoEntity entity = ActualizarGrupoRepositoryMapper.toEntity(projection);

        assertEquals(grupo, entity.id());
        assertEquals("Grupo actualizado.", entity.mensajeUsuario());
    }
}
