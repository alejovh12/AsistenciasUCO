package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoEstadoAsignaturaProjection;
import java.util.UUID;

public interface AsignaturaCommandPort {

    void crearAsignatura(UUID idAsignatura, String codigo, String nombre, Integer creditos, UUID idPlanEstudio,
                         Integer semestreNumero, String nombreArea, String nombreComponente, String codigoCaracter, UUID usuarioEjecutor);

    void actualizarAsignatura(UUID idAsignatura, String codigo, String nombre, Integer creditos, UUID idPlanEstudio,
                              Integer semestreNumero, String nombreArea, String nombreComponente, String codigoCaracter, UUID usuarioEjecutor);

    /**
     * Activa o inactiva la asignatura (HU074).
     *
     * @param estaActivaDeseada estado pedido; {@code null} alterna el estado actual.
     * @return mensaje de usuario y codigo de exito que entrego la DB (ASI_011 quedo activa / ASI_012 quedo inactiva).
     */
    ResultadoEstadoAsignaturaProjection toggleEstadoAsignatura(UUID idAsignatura, Boolean estaActivaDeseada, UUID usuarioEjecutor);
}
