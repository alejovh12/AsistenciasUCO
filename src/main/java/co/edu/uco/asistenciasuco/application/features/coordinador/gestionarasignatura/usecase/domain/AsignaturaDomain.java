package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion de guardar (crear/actualizar) asignatura.
 */
public record AsignaturaDomain(
        UUID idAsignatura,
        UUID idPlanEstudio,
        String codigo,
        String nombre,
        Integer creditos,
        Integer semestreNumero,
        String nombreArea,
        String nombreComponente
) {
}
