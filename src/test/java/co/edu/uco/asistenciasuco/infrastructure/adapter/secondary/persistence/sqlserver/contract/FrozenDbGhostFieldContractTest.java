package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.contract;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.ActualizarGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.CrearGrupoRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FrozenDbGhostFieldContractTest {

    @Test
    void grupo_no_expone_aula_en_ninguna_frontera_del_command() {
        assertNoMemberNamedAula(CrearGrupoRequest.class);
        assertNoMemberNamedAula(ActualizarGrupoRequest.class);
        for (final Class<?> type : List.of(
                CrearGrupoDTO.class,
                ActualizarGrupoDTO.class,
                CrearGrupoDomain.class,
                ActualizarGrupoDomain.class,
                CrearGrupoRepositoryDTO.class,
                ActualizarGrupoRepositoryDTO.class
        )) {
            assertNoRecordComponentNamedAula(type);
        }
    }

    @Test
    void horario_estudiante_no_expone_aula_en_projection_domain_o_dto() {
        for (final Class<?> type : List.of(
                HorarioEstudianteProjection.class,
                HorarioEstudianteDomain.class,
                HorarioEstudianteDTO.class
        )) {
            assertNoRecordComponentNamedAula(type);
        }
    }

    private void assertNoMemberNamedAula(final Class<?> type) {
        assertFalse(Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.getName().equals("aula")), type.getName());
        assertFalse(Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.getName().toLowerCase().contains("aula")), type.getName());
    }

    private void assertNoRecordComponentNamedAula(final Class<?> type) {
        assertFalse(Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .anyMatch("aula"::equals), type.getName());
    }
}
