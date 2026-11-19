package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenDocentesProgramaProjection;

import java.util.UUID;

/** Puerto de salida del consolidado del directorio docente del programa del coordinador (HU095). */
public interface ResumenDocentesProgramaQueryPort {

    ResumenDocentesProgramaProjection consultarResumen(UUID usuarioEjecutor);
}
