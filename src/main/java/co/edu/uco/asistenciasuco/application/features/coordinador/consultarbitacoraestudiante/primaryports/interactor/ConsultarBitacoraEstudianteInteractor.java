package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.ConsultarBitacoraEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.ConsultarBitacoraEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.mapper.ConsultarBitacoraEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.ConsultarBitacoraEstudianteUseCase;
import java.util.Objects;

public class ConsultarBitacoraEstudianteInteractor implements ConsultarBitacoraEstudianteInputPort {

    private final ConsultarBitacoraEstudianteUseCase useCase;

    public ConsultarBitacoraEstudianteInteractor(final ConsultarBitacoraEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public BitacoraPaginaDTO execute(final ConsultarBitacoraEstudianteDTO dto) {
        return ConsultarBitacoraEstudianteMapper.toDto(useCase.execute(ConsultarBitacoraEstudianteMapper.toDomain(dto)));
    }
}
