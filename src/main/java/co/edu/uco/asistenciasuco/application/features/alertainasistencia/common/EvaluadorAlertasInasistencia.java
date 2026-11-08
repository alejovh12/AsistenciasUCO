package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.util.UUID;

/**
 * CULM-001 C.1 ([SUPUESTO-C-04]): evaluacion de alertas de un grupo con sus avisos. Los casos de uso que escriben asistencia la invocan
 * <b>despues</b> de confirmada la escritura; nunca propaga un fallo (se registra y la operacion original no cambia).
 */
public interface EvaluadorAlertasInasistencia {

    /** Evalua el grupo (o solo la matricula si no es nula) y avisa. */
    void evaluarYAvisar(UUID grupoId, UUID estudianteGrupoId);

    /** Evalua con aviso todos los grupos del periodo (antes del cierre masivo). */
    void evaluarPeriodoYAvisar(UUID periodoId);

    /** Evaluador nulo: las construcciones anteriores a C.1 no evaluan. */
    EvaluadorAlertasInasistencia NINGUNO = new EvaluadorAlertasInasistencia() {
        @Override
        public void evaluarYAvisar(final UUID grupoId, final UUID estudianteGrupoId) {
            // sin evaluacion
        }

        @Override
        public void evaluarPeriodoYAvisar(final UUID periodoId) {
            // sin evaluacion
        }
    };
}
