package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoEstadoAsignaturaProjection;
import java.util.UUID;

public interface AsignaturaCommandPort {

    void crearAsignatura(UUID idAsignatura, String codigo, String nombre, Integer creditos, UUID idPlanEstudio,
                         Integer semestreNumero, String nombreArea, String nombreComponente, String codigoCaracter, Integer horasSemanales,
                         UUID usuarioEjecutor);

    void actualizarAsignatura(UUID idAsignatura, String codigo, String nombre, Integer creditos, UUID idPlanEstudio,
                              Integer semestreNumero, String nombreArea, String nombreComponente, String codigoCaracter, Integer horasSemanales,
                         UUID usuarioEjecutor);

    /** HU074: {@code estaActivaDeseada} nulo conserva el comportamiento compatible de alternar. */
    ResultadoEstadoAsignaturaProjection toggleEstadoAsignatura(UUID idAsignatura, Boolean estaActivaDeseada, UUID usuarioEjecutor);
}
