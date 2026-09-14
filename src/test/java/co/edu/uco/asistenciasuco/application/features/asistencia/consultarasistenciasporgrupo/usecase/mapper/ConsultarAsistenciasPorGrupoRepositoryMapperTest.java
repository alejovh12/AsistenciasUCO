package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarAsistenciasPorGrupoRepositoryMapperTest {

    @Test
    void toRepositoryDTO_con_dominio_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarAsistenciasPorGrupoRepositoryMapper.toRepositoryDTO(null));
    }

    @Test
    void toRepositoryDTO_con_dominio_valido_mapea_grupo_y_sesion() {
        final UUID grupo = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final ConsultarAsistenciasPorGrupoDomain domain = new ConsultarAsistenciasPorGrupoDomain(grupo, sesion);

        final ConsultarAsistenciasPorGrupoRepositoryDTO dto = ConsultarAsistenciasPorGrupoRepositoryMapper.toRepositoryDTO(domain);

        assertEquals(grupo, dto.getGrupo());
        assertEquals(sesion, dto.getSesion());
    }

    @Test
    void toUseCaseEntities_con_lista_nula_retorna_lista_vacia() {
        assertTrue(ConsultarAsistenciasPorGrupoRepositoryMapper.toUseCaseEntities(null).isEmpty());
    }

    @Test
    void toUseCaseEntities_mapea_proyeccion_completa() {
        final AsistenciaRepositoryProjection projection = new AsistenciaRepositoryProjection(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true, "A tiempo");

        final List<AsistenciaConsultadaEntity> resultado =
                ConsultarAsistenciasPorGrupoRepositoryMapper.toUseCaseEntities(List.of(projection));

        assertEquals(1, resultado.size());
        assertEquals(projection.getAsistencia(), resultado.getFirst().getAsistencia());
        assertTrue(resultado.getFirst().isPresente());
    }
}
