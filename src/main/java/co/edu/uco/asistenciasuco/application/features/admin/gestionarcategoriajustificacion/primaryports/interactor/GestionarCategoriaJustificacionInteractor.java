package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.GestionarCategoriaJustificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.CategoriaJustificacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.GuardarCategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.GestionarCategoriaJustificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.domain.CategoriaJustificacionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GestionarCategoriaJustificacionInteractor implements GestionarCategoriaJustificacionInputPort {

    private final GestionarCategoriaJustificacionUseCase useCase;

    public GestionarCategoriaJustificacionInteractor(final GestionarCategoriaJustificacionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "GestionarCategoriaJustificacionUseCase es obligatorio.");
    }

    @Override
    public List<CategoriaJustificacionAdminDTO> consultar() {
        return useCase.consultar().stream().map(p -> new CategoriaJustificacionAdminDTO(p.id(), p.codigo(), p.nombre(), p.descripcion(), p.requiereSoporte(), p.estaActivo())).toList();
    }

    @Override
    public ResultadoCatalogoAdminDTO crear(final GuardarCategoriaJustificacionDTO dto) {
        return resultado(useCase.crear(dominio(dto)));
    }

    @Override
    public ResultadoCatalogoAdminDTO actualizar(final GuardarCategoriaJustificacionDTO dto) {
        return resultado(useCase.actualizar(dominio(dto)));
    }

    @Override
    public ResultadoCatalogoAdminDTO cambiarEstado(final UUID id, final Boolean estaActiva, final UUID usuarioEjecutor) {
        return resultado(useCase.cambiarEstado(id, estaActiva, usuarioEjecutor));
    }

    private static CategoriaJustificacionDomain dominio(final GuardarCategoriaJustificacionDTO dto) {
        return new CategoriaJustificacionDomain(dto.id(), dto.codigo(), dto.nombre(), dto.descripcion(), dto.requiereSoporte(), dto.usuario());
    }

    private static ResultadoCatalogoAdminDTO resultado(final ResultadoCatalogoAdminProjection r) {
        return new ResultadoCatalogoAdminDTO(r.id(), r.mensajeUsuario());
    }
}
