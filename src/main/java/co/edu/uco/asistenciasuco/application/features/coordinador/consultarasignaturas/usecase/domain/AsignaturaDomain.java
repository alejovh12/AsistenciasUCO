package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de asignaturas del coordinador.
 */
public record AsignaturaDomain(
        UUID id,
        String codigo,
        String nombre,
        Integer credito,
        UUID idArea,
        String nombreArea,
        UUID idComponente,
        String nombreComponente,
        UUID idSemestrePlanEstudio,
        UUID idPlanEstudio,
        UUID idPrograma,
        String nombrePrograma,
        String codigoSemestre,
        boolean estaActivaAsignatura,
        String estaActivaTextoAsignatura
) {
}
