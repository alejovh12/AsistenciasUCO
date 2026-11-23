package co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.ConsultarCatalogosSistemaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.CategoriaJustificacionCatalogoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.MotivoCancelacionCatalogoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.usecase.ConsultarCatalogosSistemaUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarCatalogosSistemaInteractor implements ConsultarCatalogosSistemaInputPort {

    private final ConsultarCatalogosSistemaUseCase useCase;

    public ConsultarCatalogosSistemaInteractor(final ConsultarCatalogosSistemaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarCatalogosSistemaUseCase es obligatorio.");
    }

    @Override
    public List<CategoriaJustificacionCatalogoDTO> consultarCategoriasJustificacion() {
        return useCase.consultarCategoriasJustificacion().stream()
                .map(p -> new CategoriaJustificacionCatalogoDTO(p.id(), p.codigo(), p.nombre(), p.descripcion(), p.requiereSoporte())).toList();
    }

    @Override
    public List<MotivoCancelacionCatalogoDTO> consultarMotivosCancelacion() {
        return useCase.consultarMotivosCancelacion().stream()
                .map(p -> new MotivoCancelacionCatalogoDTO(p.id(), p.codigo(), p.nombre(), p.orden(), p.estaActivo())).toList();
    }
}
