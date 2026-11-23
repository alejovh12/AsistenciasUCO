package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionCatalogoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionCatalogoProjection;

import java.util.List;

/** Lectura de los catalogos definidos por el sistema: categorias de justificacion y motivos de cancelacion de sesion. */
public interface CatalogosSistemaConsultaPort {

    List<CategoriaJustificacionCatalogoProjection> consultarCategoriasJustificacion();

    /** Todos los motivos, con su estado. */
    List<MotivoCancelacionCatalogoProjection> consultarMotivosCancelacion();
}
