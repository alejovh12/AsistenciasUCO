package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface PlanEstudioCommandPort {

    /**
     * Registra o actualiza un plan de estudio. {@code idUsuarioEjecutor} es el coordinador autenticado: el
     * provider DB lo revalida (perfil COORDINADOR y titularidad del programa) y no confia solo en Application.
     */
    void registrarOActualizarPlanEstudio(UUID idPlanEstudio, UUID idPrograma, Integer inp, UUID idUsuarioEjecutor);

    /** HU081: activa o inactiva el plan. El provider DB revalida perfil COORDINADOR y titularidad del programa del plan. */
    void cambiarEstadoPlanEstudio(UUID idPlanEstudio, boolean estaActivo, UUID idUsuarioEjecutor);

    /** HU082: agrega al plan el siguiente semestre consecutivo. */
    void agregarSemestrePlanEstudio(UUID idPlanEstudio, UUID idUsuarioEjecutor);

    /** HU083: quita el ultimo semestre del plan si no tiene asignaturas. */
    void quitarSemestrePlanEstudio(UUID idPlanEstudio, Integer numeroSemestre, UUID idUsuarioEjecutor);
}
