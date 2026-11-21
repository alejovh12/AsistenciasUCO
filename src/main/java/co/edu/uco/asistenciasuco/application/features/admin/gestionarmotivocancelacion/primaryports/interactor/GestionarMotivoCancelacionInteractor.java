package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.GestionarMotivoCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.MotivoCancelacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.GuardarMotivoCancelacionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.GestionarMotivoCancelacionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.domain.MotivoCancelacionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GestionarMotivoCancelacionInteractor implements GestionarMotivoCancelacionInputPort {

    private final GestionarMotivoCancelacionUseCase useCase;

    public GestionarMotivoCancelacionInteractor(final GestionarMotivoCancelacionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "GestionarMotivoCancelacionUseCase es obligatorio.");
    }

    @Override
    public List<MotivoCancelacionAdminDTO> consultar() {
        return useCase.consultar().stream().map(p -> new MotivoCancelacionAdminDTO(p.id(), p.codigo(), p.nombre(), p.orden(), p.estaActivo())).toList();
    }

    @Override
    public ResultadoCatalogoAdminDTO crear(final GuardarMotivoCancelacionDTO dto) {
        return resultado(useCase.crear(dominio(dto)));
    }

    @Override
    public ResultadoCatalogoAdminDTO actualizar(final GuardarMotivoCancelacionDTO dto) {
        return resultado(useCase.actualizar(dominio(dto)));
    }

    @Override
    public ResultadoCatalogoAdminDTO cambiarEstado(final UUID id, final Boolean estaActivo, final UUID usuarioEjecutor) {
        return resultado(useCase.cambiarEstado(id, estaActivo, usuarioEjecutor));
    }

    private static MotivoCancelacionDomain dominio(final GuardarMotivoCancelacionDTO dto) {
        return new MotivoCancelacionDomain(dto.id(), dto.codigo(), dto.nombre(), dto.orden(), dto.usuario());
    }

    private static ResultadoCatalogoAdminDTO resultado(final ResultadoCatalogoAdminProjection r) {
        return new ResultadoCatalogoAdminDTO(r.id(), r.mensajeUsuario());
    }
}
