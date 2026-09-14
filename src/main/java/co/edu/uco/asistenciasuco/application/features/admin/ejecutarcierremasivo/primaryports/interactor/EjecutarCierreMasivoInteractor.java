package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.EjecutarCierreMasivoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.dto.EjecutarCierreMasivoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.mapper.EjecutarCierreMasivoMapper;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.EjecutarCierreMasivoUseCase;

import java.util.Objects;

public final class EjecutarCierreMasivoInteractor implements EjecutarCierreMasivoInputPort {
    private final EjecutarCierreMasivoUseCase useCase;
    public EjecutarCierreMasivoInteractor(final EjecutarCierreMasivoUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public void execute(final EjecutarCierreMasivoDTO dto) { useCase.execute(EjecutarCierreMasivoMapper.toDomain(dto)); }
}
