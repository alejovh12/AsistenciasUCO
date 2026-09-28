package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.EjecutarCierreMasivoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.domain.EjecutarCierreMasivoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.mapper.EjecutarCierreMasivoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;

import java.util.Objects;

public final class EjecutarCierreMasivoUseCaseImpl implements EjecutarCierreMasivoUseCase {
    private final PeriodoAcademicoQueryPort periodoAcademicoQueryPort;
    private final CierrePeriodoCommandPort cierrePeriodoCommandPort;
    public EjecutarCierreMasivoUseCaseImpl(final PeriodoAcademicoQueryPort periodoAcademicoQueryPort,
                                           final CierrePeriodoCommandPort cierrePeriodoCommandPort) {
        this.periodoAcademicoQueryPort = Objects.requireNonNull(periodoAcademicoQueryPort);
        this.cierrePeriodoCommandPort = Objects.requireNonNull(cierrePeriodoCommandPort);
    }
    @Override public void execute(final EjecutarCierreMasivoDomain domain) {
        final var periodo = periodoAcademicoQueryPort.consultarPeriodoAcademicoPorId(domain.getIdPeriodoAcademico())
                .orElseThrow(() -> new ResourceNotFoundException("El periodo academico indicado no existe."));
        cierrePeriodoCommandPort.ejecutarCierreMasivoPeriodo(
                EjecutarCierreMasivoRepositoryMapper.toCodigoPeriodo(periodo),
                domain.getActorUsuarioId().toString(),
                domain.getActorUsuarioId()
        );
    }
}
