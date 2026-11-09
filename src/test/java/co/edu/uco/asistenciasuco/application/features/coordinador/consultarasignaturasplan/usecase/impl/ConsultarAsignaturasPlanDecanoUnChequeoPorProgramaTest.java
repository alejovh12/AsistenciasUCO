package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PAG-DECANO-PLAN (RED): el decano valida el ambito de cada programa distinto del plan una sola vez, no una consulta de
 * ambito por asignatura. El resultado, el 403 y su mensaje no cambian.
 */
class ConsultarAsignaturasPlanDecanoUnChequeoPorProgramaTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID P1 = UUID.randomUUID();
    private static final UUID P2 = UUID.randomUUID();
    private static final UUID AJENO = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final AsignaturaQueryPort queryPort = mock(AsignaturaQueryPort.class);
    private final ConsultarAsignaturasPlanUseCaseImpl useCase = new ConsultarAsignaturasPlanUseCaseImpl(scopePort, queryPort);

    private static AsignaturaProjection asignatura(final UUID idPrograma) {
        return new AsignaturaProjection(UUID.randomUUID(), "A1", "Algebra", 3, UUID.randomUUID(), "Area", UUID.randomUUID(),
                "Componente", UUID.randomUUID(), PLAN, idPrograma, "Sistemas", "S1", true, "SI");
    }

    private void comoDecano(final List<AsignaturaProjection> asignaturas) {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarAsignaturasPorPlan(PLAN)).thenReturn(asignaturas);
    }

    @Test
    @DisplayName("tres asignaturas del mismo programa: una sola validacion de ambito")
    void mismo_programa_se_valida_una_vez() {
        comoDecano(List.of(asignatura(P1), asignatura(P1), asignatura(P1)));
        when(scopePort.canDecanoAccessPrograma(ACTOR, P1)).thenReturn(true);

        assertEquals(3, useCase.execute(ACTOR, PLAN).size());

        verify(scopePort, times(1)).canDecanoAccessPrograma(ACTOR, P1);
    }

    @Test
    @DisplayName("cada programa distinto se valida una vez aunque se repita entre asignaturas")
    void cada_programa_distinto_se_valida_una_vez() {
        comoDecano(List.of(asignatura(P1), asignatura(P2), asignatura(P1), asignatura(P2)));
        when(scopePort.canDecanoAccessPrograma(ACTOR, P1)).thenReturn(true);
        when(scopePort.canDecanoAccessPrograma(ACTOR, P2)).thenReturn(true);

        assertEquals(4, useCase.execute(ACTOR, PLAN).size());

        verify(scopePort, times(1)).canDecanoAccessPrograma(ACTOR, P1);
        verify(scopePort, times(1)).canDecanoAccessPrograma(ACTOR, P2);
    }

    @Test
    @DisplayName("un plan grande de un solo programa no genera una consulta de ambito por asignatura")
    void plan_grande_de_un_programa_no_escala_con_el_numero_de_asignaturas() {
        comoDecano(IntStream.range(0, 60).mapToObj(i -> asignatura(P1)).toList());
        when(scopePort.canDecanoAccessPrograma(ACTOR, P1)).thenReturn(true);

        assertEquals(60, useCase.execute(ACTOR, PLAN).size());

        verify(scopePort, times(1)).canDecanoAccessPrograma(any(), any());
    }

    @Test
    @DisplayName("un programa ajeno entre varios rechaza con el mismo 403, mensaje y una sola consulta sobre ese programa")
    void programa_ajeno_rechaza_con_el_mismo_error() {
        comoDecano(List.of(asignatura(P1), asignatura(AJENO), asignatura(P1), asignatura(AJENO)));
        when(scopePort.canDecanoAccessPrograma(ACTOR, P1)).thenReturn(true);
        when(scopePort.canDecanoAccessPrograma(ACTOR, AJENO)).thenReturn(false);

        final ForbiddenException error = assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR, PLAN));

        assertEquals("El plan de estudio solicitado no pertenece al ambito del usuario autenticado.", error.getMessage());
        verify(scopePort, times(1)).canDecanoAccessPrograma(ACTOR, AJENO);
    }

    @Test
    @DisplayName("el coordinador sigue comparando en memoria: nunca consulta el ambito de decano")
    void coordinador_no_consulta_ambito_de_decano() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(P1));
        when(queryPort.consultarAsignaturasPorPlan(PLAN)).thenReturn(List.of(asignatura(P1), asignatura(P1)));

        assertEquals(2, useCase.execute(ACTOR, PLAN).size());

        verify(scopePort, never()).canDecanoAccessPrograma(any(), any());
    }
}
