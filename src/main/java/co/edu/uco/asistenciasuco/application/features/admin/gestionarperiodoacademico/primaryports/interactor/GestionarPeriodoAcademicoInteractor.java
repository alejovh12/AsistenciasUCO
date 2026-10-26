package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.GestionarPeriodoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.mapper.GestionarPeriodoAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.GestionarPeriodoAcademicoUseCase;

import java.util.Objects;
import java.util.UUID;

public final class GestionarPeriodoAcademicoInteractor implements GestionarPeriodoAcademicoInputPort {

    private final GestionarPeriodoAcademicoUseCase useCase;

    public GestionarPeriodoAcademicoInteractor(final GestionarPeriodoAcademicoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "GestionarPeriodoAcademicoUseCase es obligatorio.");
    }

    @Override
    public void crear(final GuardarPeriodoAcademicoDTO dto) {
        useCase.crear(GestionarPeriodoAcademicoMapper.toDomain(dto));
    }

    @Override
    public void actualizar(final GuardarPeriodoAcademicoDTO dto) {
        useCase.actualizar(GestionarPeriodoAcademicoMapper.toDomain(dto));
    }

    @Override
    public void eliminar(final UUID idPeriodo, final UUID usuario) {
        useCase.eliminar(idPeriodo, usuario);
    }
}
