package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

public final class GenerarSesionesGrupoRepositoryMapper {

    private GenerarSesionesGrupoRepositoryMapper() {
    }

    public static GenerarSesionesGrupoRepositoryDTO toRepositoryDTO(final GenerarSesionesGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para generar sesiones de grupo es obligatorio.");
        }
        return new GenerarSesionesGrupoRepositoryDTO(domain.getGrupo());
    }
}
