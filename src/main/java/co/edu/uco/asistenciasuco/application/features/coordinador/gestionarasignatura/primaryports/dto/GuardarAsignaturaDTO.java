package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto;

import java.util.UUID;

public record GuardarAsignaturaDTO(
        UUID idAsignatura,
        UUID idPlanEstudio,
        String codigo,
        String nombre,
        Integer creditos,
        Integer semestreNumero,
        String nombreArea,
        String nombreComponente,
        String codigoCaracter,
        UUID usuarioEjecutor,
        Integer horasSemanales
) {
    /** Constructor previo a SES-GEN-01: sin horas semanales (en actualizar significa "no cambia"). */
    public GuardarAsignaturaDTO(final UUID idAsignatura, final UUID idPlanEstudio, final String codigo, final String nombre, final Integer creditos,
                                final Integer semestreNumero, final String nombreArea, final String nombreComponente, final String codigoCaracter,
                                final UUID usuarioEjecutor) {
        this(idAsignatura, idPlanEstudio, codigo, nombre, creditos, semestreNumero, nombreArea, nombreComponente, codigoCaracter, usuarioEjecutor, null);
    }
}
