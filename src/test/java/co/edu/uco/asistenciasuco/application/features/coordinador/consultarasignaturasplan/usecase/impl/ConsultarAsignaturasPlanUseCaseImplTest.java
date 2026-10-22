package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Asignaturas de un plan por rol. El coordinador solo ve planes de su programa; el decano, planes de programas de su
 * facultad (se valida con {@code canDecanoAccessPrograma}). Un plan ajeno responde 403 para ambos.
 */
class ConsultarAsignaturasPlanUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID PROGRAMA_AJENO = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final AsignaturaQueryPort queryPort = mock(AsignaturaQueryPort.class);
    private final ConsultarAsignaturasPlanUseCaseImpl useCase = new ConsultarAsignaturasPlanUseCaseImpl(scopePort, queryPort);

    private static AsignaturaProjection asignatura(final UUID idPrograma) {
        return new AsignaturaProjection(UUID.randomUUID(), "A1", "Algebra", 3, UUID.randomUUID(), "Area", UUID.randomUUID(),
                "Componente", UUID.randomUUID(), PLAN, idPrograma, "Sistemas", "S1", true, "SI");
    }

    @Test
    void coordinador_consulta_las_asignaturas_de_un_plan_de_su_programa() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarAsignaturasPorPlan(PLAN)).thenReturn(List.of(asignatura(PROGRAMA)));

        final List<AsignaturaDomain> resultado = useCase.execute(ACTOR, PLAN);

        assertEquals(1, resultado.size());
    }

    @Test
    void coordinador_con_un_plan_de_otro_programa_recibe_forbidden() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarAsignaturasPorPlan(PLAN)).thenReturn(List.of(asignatura(PROGRAMA_AJENO)));

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR, PLAN));
    }

    @Test
    void decano_consulta_un_plan_cuyo_programa_es_de_su_facultad() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarAsignaturasPorPlan(PLAN)).thenReturn(List.of(asignatura(PROGRAMA), asignatura(PROGRAMA)));
        when(scopePort.canDecanoAccessPrograma(ACTOR, PROGRAMA)).thenReturn(true);

        final List<AsignaturaDomain> resultado = useCase.execute(ACTOR, PLAN);

        assertEquals(2, resultado.size());
    }

    @Test
    void decano_con_un_plan_de_otra_facultad_recibe_forbidden() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarAsignaturasPorPlan(PLAN)).thenReturn(List.of(asignatura(PROGRAMA_AJENO)));
        when(scopePort.canDecanoAccessPrograma(ACTOR, PROGRAMA_AJENO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR, PLAN));
    }

    @Test
    void decano_con_un_plan_sin_asignaturas_obtiene_lista_vacia() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarAsignaturasPorPlan(PLAN)).thenReturn(List.of());

        assertEquals(0, useCase.execute(ACTOR, PLAN).size());
        verify(scopePort, never()).canDecanoAccessPrograma(any(), any());
    }

    @Test
    void sin_programa_ni_facultad_se_rechaza_sin_consultar() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR, PLAN));

        verifyNoInteractions(queryPort);
    }
}
