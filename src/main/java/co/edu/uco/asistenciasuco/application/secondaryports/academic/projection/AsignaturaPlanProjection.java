package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

/** Vinculo asignatura-plan (CULM-001 B2NM). */
public record AsignaturaPlanProjection(java.util.UUID idAsignatura, String codigo, String nombre, Integer credito, java.util.UUID idArea, String nombreArea,
        java.util.UUID idComponente, String nombreComponente, java.util.UUID idPlanEstudio, Integer inpPlanEstudio, java.util.UUID idPrograma,
        String nombrePrograma, java.util.UUID idSemestrePlanEstudio, Integer numeroSemestre, String codigoSemestre, String codigoCaracter,
        String nombreCaracter, boolean estaActivoVinculo, boolean estaActivaAsignatura, boolean compartida,
        Integer horasSemanales) {
    /** Constructor previo a SES-GEN-01: horas semanales sin cargar (NULL). */
    public AsignaturaPlanProjection(java.util.UUID idAsignatura, String codigo, String nombre, Integer credito, java.util.UUID idArea, String nombreArea,
            java.util.UUID idComponente, String nombreComponente, java.util.UUID idPlanEstudio, Integer inpPlanEstudio, java.util.UUID idPrograma,
            String nombrePrograma, java.util.UUID idSemestrePlanEstudio, Integer numeroSemestre, String codigoSemestre, String codigoCaracter,
            String nombreCaracter, boolean estaActivoVinculo, boolean estaActivaAsignatura, boolean compartida) {
        this(idAsignatura, codigo, nombre, credito, idArea, nombreArea, idComponente, nombreComponente, idPlanEstudio, inpPlanEstudio, idPrograma,
                nombrePrograma, idSemestrePlanEstudio, numeroSemestre, codigoSemestre, codigoCaracter, nombreCaracter, estaActivoVinculo,
                estaActivaAsignatura, compartida, null);
    }
}
