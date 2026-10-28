package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HistorialGruposDocentePagina;

import java.util.UUID;

/** Puerto de salida de lectura del historial de grupos de un docente en el programa del coordinador (HU094). */
public interface HistorialGruposDocenteQueryPort {

    HistorialGruposDocentePagina consultarHistorial(UUID usuarioEjecutor, UUID idDocente, UUID idPeriodoAcademico, int pagina, int tamano);
}
