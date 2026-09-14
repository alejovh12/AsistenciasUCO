package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarDocentesRepositoryMapperTest {

    @Test
    void toUseCaseEntity_con_entidad_nula_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarDocentesRepositoryMapper.toUseCaseEntity(null));
    }

    @Test
    void toUseCaseEntities_con_lista_nula_retorna_lista_vacia() {
        assertTrue(ConsultarDocentesRepositoryMapper.toUseCaseEntities(null).isEmpty());
    }

    @Test
    void toUseCaseEntities_mapea_proyeccion_completa() {
        final UUID id = UUID.randomUUID();
        final DocenteIdentidadRepositoryProjection projection = new DocenteIdentidadRepositoryProjection(
                id, UUID.randomUUID(), 123456789, "Ana Perez", true);

        final List<DocenteIdentidadEntity> resultado = ConsultarDocentesRepositoryMapper.toUseCaseEntities(List.of(projection));

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().getId());
        assertTrue(resultado.getFirst().isEstaActivoUsuario());
    }
}
