package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.ConsultarAlertaTempranaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.ConsultarAlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.mapper.ConsultarAlertaTempranaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.ConsultarAlertaTempranaUseCase;
import java.util.Objects;

public class ConsultarAlertaTempranaInteractor implements ConsultarAlertaTempranaInputPort {

    private final ConsultarAlertaTempranaUseCase useCase;

    public ConsultarAlertaTempranaInteractor(final ConsultarAlertaTempranaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public AlertasPaginaDTO execute(final ConsultarAlertaTempranaDTO dto) {
        return ConsultarAlertaTempranaMapper.toDto(useCase.execute(ConsultarAlertaTempranaMapper.toDomain(dto)));
    }
}
