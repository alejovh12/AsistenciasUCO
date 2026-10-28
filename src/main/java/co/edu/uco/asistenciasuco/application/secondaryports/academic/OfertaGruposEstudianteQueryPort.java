package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.OfertaGruposEstudiantePagina;

import java.util.UUID;

/** Puerto de salida de la oferta de grupos del estudiante autenticado (HU008). El ambito lo fuerza el SP. */
public interface OfertaGruposEstudianteQueryPort {

    OfertaGruposEstudiantePagina consultarOferta(UUID usuarioEjecutor, String busqueda, int pagina, int tamano);
}
