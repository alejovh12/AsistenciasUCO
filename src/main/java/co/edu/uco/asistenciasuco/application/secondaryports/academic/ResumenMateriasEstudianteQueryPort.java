package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenMateriasEstudianteProjection;

import java.util.UUID;

/** CIERRE-W2B: puerto de salida del resumen del tablero del estudiante (calculado en la base). */
public interface ResumenMateriasEstudianteQueryPort {

    /** Recibe el USUARIO ejecutor; el SP resuelve el estudiante y usa el mismo conjunto de materias que el listado. */
    ResumenMateriasEstudianteProjection consultarResumen(UUID usuarioEjecutor);
}
