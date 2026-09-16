package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.domain.EjecutarCierreMasivoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
}
