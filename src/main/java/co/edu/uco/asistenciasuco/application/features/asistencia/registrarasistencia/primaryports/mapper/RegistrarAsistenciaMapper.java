package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain.RegistrarAsistenciaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir el DTO de entrada al dominio del caso de uso.
 */
public final class RegistrarAsistenciaMapper {

    private RegistrarAsistenciaMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static RegistrarAsistenciaDomain toDomain(final RegistrarAsistenciaDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar asistencia es obligatorio.");
        }

        return new RegistrarAsistenciaDomain(
                dto.getEstudiante(),
                dto.getGrupo(),
                dto.getSesion(),
                dto.getPresente(),
                dto.getObservacion(),
                dto.getIdCorrelacion()
        );
    }
}
