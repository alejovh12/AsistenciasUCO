package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.ConsultarFichaDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ConsultarFichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.FichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.mapper.ConsultarFichaDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.ConsultarFichaDocenteUseCase;

import java.util.Objects;

public class ConsultarFichaDocenteInteractor implements ConsultarFichaDocenteInputPort {

    private final ConsultarFichaDocenteUseCase useCase;

    public ConsultarFichaDocenteInteractor(final ConsultarFichaDocenteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public FichaDocenteDTO execute(final ConsultarFichaDocenteDTO dto) {
        return ConsultarFichaDocenteMapper.toDto(useCase.execute(ConsultarFichaDocenteMapper.toDomain(dto)));
    }
}
