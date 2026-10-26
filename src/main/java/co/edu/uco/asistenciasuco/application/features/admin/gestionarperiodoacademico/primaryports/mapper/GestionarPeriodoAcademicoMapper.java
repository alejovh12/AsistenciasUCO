package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.domain.PeriodoAcademicoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper entre el DTO de entrada y el dominio de gestion de periodos academicos.
 */
public final class GestionarPeriodoAcademicoMapper {

    private GestionarPeriodoAcademicoMapper() {
    }

    public static PeriodoAcademicoDomain toDomain(final GuardarPeriodoAcademicoDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para guardar periodo academico es obligatorio.");
        }
        return new PeriodoAcademicoDomain(dto.idPeriodo(), dto.nombre(), dto.fechaInicio(), dto.fechaFin(), dto.usuario());
    }
}
