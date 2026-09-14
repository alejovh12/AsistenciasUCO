package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.entity.ActualizarGrupoResultadoEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActualizarGrupoMapperTest {

    @Test
    void toDomain_mapea_todos_los_campos() {
        final UUID grupo = UUID.randomUUID();
        final ActualizarGrupoDTO dto = new ActualizarGrupoDTO(grupo, 1, "Grupo 1", UUID.randomUUID(), 30, "Aula 1");

        final ActualizarGrupoDomain domain = ActualizarGrupoMapper.toDomain(dto);

        assertEquals(grupo, domain.idGrupo());
        assertEquals(30, domain.cupoMaximo());
    }

    @Test
    void toDTO_mapea_entidad_resultado() {
        final UUID id = UUID.randomUUID();
        final ActualizarGrupoResultadoEntity entity = new ActualizarGrupoResultadoEntity(id, "Grupo actualizado.");

        final ActualizarGrupoResultadoDTO dto = ActualizarGrupoMapper.toDTO(entity);

        assertEquals(id, dto.id());
        assertEquals("Grupo actualizado.", dto.mensajeUsuario());
    }
}
