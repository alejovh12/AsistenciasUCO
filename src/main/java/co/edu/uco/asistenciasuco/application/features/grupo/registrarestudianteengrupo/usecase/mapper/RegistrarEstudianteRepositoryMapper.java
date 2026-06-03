package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el dominio del caso de uso y el contrato del puerto secundario.
 */
public final class RegistrarEstudianteRepositoryMapper {

    private RegistrarEstudianteRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static RegistrarEstudianteRepositoryDTO toRepositoryDTO(final RegistrarEstudianteDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }

        return new RegistrarEstudianteRepositoryDTO(
                domain.getEstudiante(),
                domain.getGrupo(),
                domain.getIdCorrelacion()
        );
    }
}
