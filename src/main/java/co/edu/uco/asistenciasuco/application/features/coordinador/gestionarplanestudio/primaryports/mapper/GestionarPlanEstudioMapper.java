package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto.GuardarPlanEstudioDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper entre el DTO de entrada y el dominio de guardar plan de estudio.
 */
public final class GestionarPlanEstudioMapper {

    private GestionarPlanEstudioMapper() {
    }

    public static PlanEstudioDomain toDomain(final GuardarPlanEstudioDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para guardar plan de estudio es obligatorio.");
        }

        return new PlanEstudioDomain(dto.idPlanEstudio(), dto.codigo(), dto.nombre(), dto.usuario());
    }
}
