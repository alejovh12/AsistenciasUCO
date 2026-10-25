package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.ConsultarFichaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ConsultarFichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.mapper.ConsultarFichaEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.ConsultarFichaEstudianteUseCase;
import java.util.Objects;

public class ConsultarFichaEstudianteInteractor implements ConsultarFichaEstudianteInputPort {

    private final ConsultarFichaEstudianteUseCase useCase;

    public ConsultarFichaEstudianteInteractor(final ConsultarFichaEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public FichaEstudianteDTO execute(final ConsultarFichaEstudianteDTO dto) {
        return ConsultarFichaEstudianteMapper.toDto(useCase.execute(ConsultarFichaEstudianteMapper.toDomain(dto)));
    }
}
