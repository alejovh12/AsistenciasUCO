package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.mapper.RegistrarEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Interactor del puerto de entrada para registrar estudiante en grupo.
 */
public final class RegistrarEstudianteInteractor implements RegistrarEstudianteInputPort {

    private final RegistrarEstudianteUseCase useCase;

    public RegistrarEstudianteInteractor(final RegistrarEstudianteUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso RegistrarEstudianteUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public RegistrarEstudianteResultadoDTO execute(final RegistrarEstudianteDTO dto) {
        final RegistrarEstudianteDomain domain = RegistrarEstudianteMapper.toDomain(dto);
        return RegistrarEstudianteMapper.toDTO(useCase.execute(domain));
    }
}
