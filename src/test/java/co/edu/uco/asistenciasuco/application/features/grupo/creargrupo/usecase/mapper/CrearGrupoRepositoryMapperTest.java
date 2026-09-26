package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.entity.CrearGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrearGrupoRepositoryMapperTest {

    @Test
    void toRepositoryDTO_mapea_todos_los_campos_incluyendo_id_externo() {
        final UUID idGrupo = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final CrearGrupoDomain domain = new CrearGrupoDomain(asignatura, UUID.randomUUID(), 1, "Grupo 1",
                UUID.randomUUID(), true, usuarioEjecutor);

        final CrearGrupoRepositoryDTO dto = CrearGrupoRepositoryMapper.toRepositoryDTO(domain, idGrupo);

        assertEquals(idGrupo, dto.idGrupo());
        assertEquals(asignatura, dto.idAsignatura());
        assertEquals(usuarioEjecutor, dto.usuarioEjecutor());
    }

    @Test
    void toEntity_mapea_proyeccion_de_comando() {
        final UUID grupo = UUID.randomUUID();
        final GrupoCommandRepositoryProjection projection = new GrupoCommandRepositoryProjection(grupo, "Grupo creado.");

        final CrearGrupoResultadoEntity entity = CrearGrupoRepositoryMapper.toEntity(projection);

        assertEquals(grupo, entity.id());
        assertEquals("Grupo creado.", entity.mensajeUsuario());
    }
}
