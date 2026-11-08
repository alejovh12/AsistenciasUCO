package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.ConsultarEstudiantesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto.ConsultarEstudiantesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto.PaginaEstudiantesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.mapper.ConsultarEstudiantesProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.ConsultarEstudiantesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.ConsultarEstudiantesProgramaDomain;
import java.util.Objects;

public final class ConsultarEstudiantesProgramaInteractor implements ConsultarEstudiantesProgramaInputPort {

    private final ConsultarEstudiantesProgramaUseCase useCase;

    public ConsultarEstudiantesProgramaInteractor(final ConsultarEstudiantesProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase);
    }

    @Override
    public PaginaEstudiantesProgramaDTO execute(final ConsultarEstudiantesProgramaDTO c) {
        final var pagina = useCase.execute(new ConsultarEstudiantesProgramaDomain(c.actorUsuarioId(), c.programaId(), c.busqueda(), c.estaActivo(), c.pagina(), c.tamano()));
        return new PaginaEstudiantesProgramaDTO(pagina.elementos().stream().map(ConsultarEstudiantesProgramaMapper::toDTO).toList(), pagina.total());
    }
}
