package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.RegistrarEstudianteRequest;

import java.util.UUID;

/**
 * Mapper del contrato HTTP al input de application.
 */
public final class RegistrarEstudianteHttpMapper {

    private RegistrarEstudianteHttpMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static RegistrarEstudianteDTO toApplicationDTO(
            final UUID grupoId,
            final RegistrarEstudianteRequest request
    ) {
        if (ObjectHelper.isNull(request)) {
            throw new CrosscuttingException("El request HTTP para registrar estudiante en grupo es obligatorio.");
        }
        return new RegistrarEstudianteDTO(
                request.getTipoIdentificacionId(),
                request.getNumeroIdentificacion(),
                request.getPrimerApellido(),
                request.getSegundoApellido(),
                request.getPrimerNombre(),
                request.getSegundoNombre(),
                request.getCorreo(),
                request.getPassword(),
                grupoId
        );
    }
}
