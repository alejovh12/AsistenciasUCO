package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocentesProgramaPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocentesProgramaPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.ConsultarDocentesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto.ConsultarDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.ConsultarDocentesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.ConsultarDocentesProgramaDomain;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarDocentesProgramaInteractor implements ConsultarDocentesProgramaInputPort {

    private final ConsultarDocentesProgramaUseCase useCase;

    public ConsultarDocentesProgramaInteractor(final ConsultarDocentesProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public DocentesProgramaPaginaDTO execute(final UUID actorUsuarioId, final ConsultarDocentesProgramaDTO consulta) {
        final DocentesProgramaPaginaDomain pagina = useCase.execute(actorUsuarioId, new ConsultarDocentesProgramaDomain(
                consulta.pagina(), consulta.tamano(), consulta.busqueda(), consulta.estaActivo(), consulta.idTipoVinculacion()));
        return new DocentesProgramaPaginaDTO(
                pagina.elementos().stream().map(DocenteProgramaMapper::toDTO).toList(), pagina.total());
    }
}
