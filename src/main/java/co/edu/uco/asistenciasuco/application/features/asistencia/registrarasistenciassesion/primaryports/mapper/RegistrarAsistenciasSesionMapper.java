package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistroAsistenciaSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;

public final class RegistrarAsistenciasSesionMapper {

    private RegistrarAsistenciasSesionMapper() {
    }

    public static RegistrarAsistenciasSesionDomain toDomain(final RegistrarAsistenciasSesionDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar asistencias de sesion es obligatorio.");
        }

        final List<RegistroAsistenciaSesionDomain> registros = dto.getRegistros() == null
                ? List.of()
                : dto.getRegistros().stream()
                        .map(registro -> new RegistroAsistenciaSesionDomain(
                                registro == null ? null : registro.getEstudiante(),
                                registro == null ? null : registro.getEstado()
                        ))
                        .toList();
        return new RegistrarAsistenciasSesionDomain(dto.getSesion(), registros);
    }
}
