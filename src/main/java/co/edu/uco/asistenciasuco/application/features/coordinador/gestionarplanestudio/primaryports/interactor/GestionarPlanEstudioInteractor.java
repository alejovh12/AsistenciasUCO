package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.GestionarPlanEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto.GuardarPlanEstudioDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.mapper.GestionarPlanEstudioMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.GestionarPlanEstudioUseCase;

import java.util.Objects;

public final class GestionarPlanEstudioInteractor implements GestionarPlanEstudioInputPort {

    private final GestionarPlanEstudioUseCase useCase;

    public GestionarPlanEstudioInteractor(final GestionarPlanEstudioUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "GestionarPlanEstudioUseCase es obligatorio.");
    }

    @Override
    public void guardar(final GuardarPlanEstudioDTO dto) {
        useCase.guardar(GestionarPlanEstudioMapper.toDomain(dto));
    }
}
