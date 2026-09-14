package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record AsignaturaProjection(
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
