package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarAsistenciasPorGrupoMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarAsistenciasPorGrupoMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_crea_dominio() {
        final UUID grupo = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();

        final UUID usuarioEjecutor = UUID.randomUUID();
        final ConsultarAsistenciasPorGrupoDomain domain =
                ConsultarAsistenciasPorGrupoMapper.toDomain(new ConsultarAsistenciasPorGrupoDTO(grupo, sesion, usuarioEjecutor));

        assertEquals(grupo, domain.getGrupo());
        assertEquals(sesion, domain.getSesion());
        assertEquals(usuarioEjecutor, domain.getUsuarioEjecutor());
    }

    @Test
    void toDTOs_con_lista_nula_retorna_lista_vacia() {
        assertTrue(ConsultarAsistenciasPorGrupoMapper.toDTOs(null).isEmpty());
    }

    @Test
    void toDTOs_mapea_entidad_completa() {
        final AsistenciaConsultadaEntity entity = new AsistenciaConsultadaEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true, "EX", "A tiempo");

        final List<AsistenciaConsultadaDTO> resultado = ConsultarAsistenciasPorGrupoMapper.toDTOs(List.of(entity));

        assertEquals(1, resultado.size());
        assertEquals(entity.getAsistencia(), resultado.getFirst().getAsistencia());
        assertTrue(resultado.getFirst().getPresente());
        assertEquals("EX", resultado.getFirst().getEstado());
    }
}
