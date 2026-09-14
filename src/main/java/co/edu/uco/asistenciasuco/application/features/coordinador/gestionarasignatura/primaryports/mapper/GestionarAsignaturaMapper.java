package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper entre el DTO de entrada y el dominio de guardar asignatura.
 */
public final class GestionarAsignaturaMapper {

    private GestionarAsignaturaMapper() {
    }

    public static AsignaturaDomain toDomain(final GuardarAsignaturaDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para guardar asignatura es obligatorio.");
        }

        return new AsignaturaDomain(
                dto.idAsignatura(),
                dto.idPlanEstudio(),
                dto.codigo(),
                dto.nombre(),
                dto.creditos(),
                dto.semestreNumero(),
                dto.nombreArea(),
                dto.nombreComponente()
        );
    }
}
