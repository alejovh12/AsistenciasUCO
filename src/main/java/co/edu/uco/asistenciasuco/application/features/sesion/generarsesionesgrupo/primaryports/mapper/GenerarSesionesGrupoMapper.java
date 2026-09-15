package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

public final class GenerarSesionesGrupoMapper {

    private GenerarSesionesGrupoMapper() {
    }

    public static GenerarSesionesGrupoDomain toDomain(final GenerarSesionesGrupoDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para generar sesiones de grupo es obligatorio.");
        }
        return new GenerarSesionesGrupoDomain(dto.getGrupo());
    }
}
