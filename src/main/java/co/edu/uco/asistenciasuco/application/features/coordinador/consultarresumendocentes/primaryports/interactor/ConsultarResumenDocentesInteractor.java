package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.ConsultarResumenDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenTipoVinculacionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.ConsultarResumenDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain.ResumenDocentesProgramaDomain;

import java.util.Objects;
import java.util.UUID;

public class ConsultarResumenDocentesInteractor implements ConsultarResumenDocentesInputPort {

    private final ConsultarResumenDocentesUseCase useCase;

    public ConsultarResumenDocentesInteractor(final ConsultarResumenDocentesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ResumenDocentesProgramaDTO execute(final UUID actorUsuarioId) {
        final ResumenDocentesProgramaDomain d = useCase.execute(actorUsuarioId);
        return new ResumenDocentesProgramaDTO(d.totalDocentes(), d.totalActivos(), d.totalInactivos(),
                d.porTipoVinculacion().stream()
                        .map(t -> new ResumenTipoVinculacionDTO(t.idTipoVinculacion(), t.codigoTipoVinculacion(),
                                t.nombreTipoVinculacion(), t.totalActivos(), t.totalInactivos(), t.total()))
                        .toList());
    }
}
