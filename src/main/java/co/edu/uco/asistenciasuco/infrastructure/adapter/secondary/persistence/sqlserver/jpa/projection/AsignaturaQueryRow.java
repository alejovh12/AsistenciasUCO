package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.util.UUID;

/**
 * Fila JPQL de asignaturas con su semestre de plan de estudio (LB-008 JPA-05).
 * {@code idPlanEstudio} e {@code idPrograma} provienen de {@code uv_semestre_plan_estudio}.
 */
public record AsignaturaQueryRow(UUID id, String codigo, String nombre, Integer credito, UUID idArea,
                                 String nombreArea, UUID idComponente, String nombreComponente,
                                 UUID idSemestrePlanEstudio, UUID idPlanEstudio, UUID idPrograma,
                                 String nombrePrograma, String codigoSemestre, Boolean estaActivaAsignatura,
                                 String estaActivaTextoAsignatura) {
}
