package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

/** Cuerpo de {@code PUT /api/v1/coordinador/estudiantes/{idEstudiante}/programas/{idPrograma}/semestre}; nulo limpia el semestre. */
public final class ActualizarSemestreEstudianteRequest {
    private Integer semestreActual;

    public Integer getSemestreActual() { return semestreActual; }
    public void setSemestreActual(final Integer semestreActual) { this.semestreActual = semestreActual; }
}
