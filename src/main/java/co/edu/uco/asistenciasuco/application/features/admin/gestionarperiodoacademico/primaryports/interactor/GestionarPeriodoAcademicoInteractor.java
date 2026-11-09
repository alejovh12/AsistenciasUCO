package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.GestionarPeriodoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.mapper.GestionarPeriodoAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.GestionarPeriodoAcademicoUseCase;

import java.util.Objects;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.PaginaPeriodosAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.PeriodoAdminDTO;
import java.util.UUID;

public final class GestionarPeriodoAcademicoInteractor implements GestionarPeriodoAcademicoInputPort {

    private final GestionarPeriodoAcademicoUseCase useCase;

    public GestionarPeriodoAcademicoInteractor(final GestionarPeriodoAcademicoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "GestionarPeriodoAcademicoUseCase es obligatorio.");
    }

    @Override
    public UUID crear(final GuardarPeriodoAcademicoDTO dto) {
        return useCase.crear(GestionarPeriodoAcademicoMapper.toDomain(dto));
    }

    @Override
    public PaginaPeriodosAdminDTO consultar(final UUID usuario, final Integer pagina, final Integer tamano) {
        final var resultado = useCase.consultar(usuario, pagina, tamano);
        return new PaginaPeriodosAdminDTO(resultado.elementos().stream()
                .map(p -> new PeriodoAdminDTO(p.id(), p.idInstitucion(), p.nombreInstitucion(), p.nombre(), p.codigo(),
                        p.fechaInicio(), p.fechaFin(), p.anio(), p.esVigente()))
                .toList(), resultado.total());
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
