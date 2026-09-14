package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarEstudiantesGrupoRepositoryMapperTest {

    @Test
    void toEntities_mapea_proyeccion_completa() {
        final UUID id = UUID.randomUUID();
        final EstudianteGrupoRepositoryProjection projection = new EstudianteGrupoRepositoryProjection(
                id, UUID.randomUUID(), "123456789", "Ana Perez", "ana@uco.edu.co", "ACTIVO", "Activo");

        final List<EstudianteGrupoEntity> resultado = ConsultarEstudiantesGrupoRepositoryMapper.toEntities(List.of(projection));

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().id());
        assertEquals("Ana Perez", resultado.getFirst().nombreCompleto());
    }
}
