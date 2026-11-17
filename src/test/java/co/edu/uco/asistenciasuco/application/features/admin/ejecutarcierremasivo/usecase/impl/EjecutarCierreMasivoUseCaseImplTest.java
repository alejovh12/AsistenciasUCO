package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.domain.EjecutarCierreMasivoDomain;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EjecutarCierreMasivoUseCaseImplTest {

    private static final UUID PERIODO_ID = UUID.randomUUID();
    private static final UUID ACTOR_USUARIO_ID = UUID.randomUUID();

    private final PeriodoAcademicoQueryPort periodoAcademicoQueryPort = mock(PeriodoAcademicoQueryPort.class);
    private final CierrePeriodoCommandPort cierrePeriodoCommandPort = mock(CierrePeriodoCommandPort.class);
    private final EjecutarCierreMasivoUseCaseImpl useCase =
            new EjecutarCierreMasivoUseCaseImpl(periodoAcademicoQueryPort, cierrePeriodoCommandPort);

    @Test
    void periodo_inexistente_falla_sin_invocar_command() {
        when(periodoAcademicoQueryPort.consultarPeriodoAcademicoPorId(PERIODO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new EjecutarCierreMasivoDomain(PERIODO_ID, ACTOR_USUARIO_ID)));

        verifyNoInteractions(cierrePeriodoCommandPort);
    }

    @Test
    void actor_usuario_id_se_propaga_como_idActor_y_idUsuarioEjecutor() {
        final PeriodoAcademicoProjection periodo = new PeriodoAcademicoProjection(
                PERIODO_ID, UUID.randomUUID(), "Institucion", "Periodo 2026-1", "2026-1", null, null, 2026
        );
        when(periodoAcademicoQueryPort.consultarPeriodoAcademicoPorId(PERIODO_ID)).thenReturn(Optional.of(periodo));

        useCase.execute(new EjecutarCierreMasivoDomain(PERIODO_ID, ACTOR_USUARIO_ID));

        verify(cierrePeriodoCommandPort).ejecutarCierreMasivoPeriodo(
                "2026-1", ACTOR_USUARIO_ID.toString(), ACTOR_USUARIO_ID
        );
    }

    /** FIX-CIERRE-002: un periodo que aun no termino no evalua alertas ni avisa; el SP lo rechaza con PER_007. */
    @Test
    void periodo_no_terminado_no_evalua_alertas_y_deja_que_la_base_lo_rechace() {
        final EvaluadorAlertasInasistencia evaluador = mock(EvaluadorAlertasInasistencia.class);
        final var conEvaluador = new EjecutarCierreMasivoUseCaseImpl(
                periodoAcademicoQueryPort, cierrePeriodoCommandPort, notificacion -> { }, evaluador);
        for (final String estado : new String[]{"ACTIVO", "PLANEACION"}) {
            when(periodoAcademicoQueryPort.consultarPeriodoAcademicoPorId(PERIODO_ID))
                    .thenReturn(Optional.of(periodoConEstado(estado)));

            conEvaluador.execute(new EjecutarCierreMasivoDomain(PERIODO_ID, ACTOR_USUARIO_ID));
        }

        verify(evaluador, never()).evaluarPeriodoYAvisar(PERIODO_ID);
        verify(cierrePeriodoCommandPort, org.mockito.Mockito.times(2)).ejecutarCierreMasivoPeriodo(
                "2026-1", ACTOR_USUARIO_ID.toString(), ACTOR_USUARIO_ID);
    }

    @Test
    void periodo_terminado_evalua_alertas_antes_del_cierre() {
        final EvaluadorAlertasInasistencia evaluador = mock(EvaluadorAlertasInasistencia.class);
        final var conEvaluador = new EjecutarCierreMasivoUseCaseImpl(
                periodoAcademicoQueryPort, cierrePeriodoCommandPort, notificacion -> { }, evaluador);
        when(periodoAcademicoQueryPort.consultarPeriodoAcademicoPorId(PERIODO_ID))
                .thenReturn(Optional.of(periodoConEstado("CERRADO")));

        conEvaluador.execute(new EjecutarCierreMasivoDomain(PERIODO_ID, ACTOR_USUARIO_ID));

        verify(evaluador).evaluarPeriodoYAvisar(PERIODO_ID);
    }

    private static PeriodoAcademicoProjection periodoConEstado(final String estado) {
        return new PeriodoAcademicoProjection(
                PERIODO_ID, UUID.randomUUID(), "Institucion", "Periodo 2026-1", "2026-1", null, null, 2026, estado);
    }
}
