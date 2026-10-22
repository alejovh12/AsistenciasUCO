package co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.ConsultarCatalogosProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.ConsultarCatalogosProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.domain.CatalogoProgramaDomain;
import java.util.List;
import java.util.Objects;

public class ConsultarCatalogosProgramaInteractor implements ConsultarCatalogosProgramaInputPort {

    private final ConsultarCatalogosProgramaUseCase useCase;

    public ConsultarCatalogosProgramaInteractor(final ConsultarCatalogosProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<CatalogoProgramaDTO> tiposPrograma() {
        return toDto(useCase.tiposPrograma());
    }

    @Override
    public List<CatalogoProgramaDTO> modalidades() {
        return toDto(useCase.modalidades());
    }

    @Override
    public List<CatalogoProgramaDTO> jornadas() {
        return toDto(useCase.jornadas());
    }

    private static List<CatalogoProgramaDTO> toDto(final List<CatalogoProgramaDomain> items) {
        return items.stream().map(i -> new CatalogoProgramaDTO(i.id(), i.codigo(), i.nombre())).toList();
    }
}
