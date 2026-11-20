package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** HU176B (CA-04): un estudiante con sesion iniciada se matricula con un token o PIN vigente. */
public interface MatricularEstudianteAutenticadoInputPort
        extends InteractorWithReturn<MatricularEstudianteAutenticadoInputPort.Comando, MatricularEstudianteAutenticadoInputPort.Resultado> {

    /** {@code programaId}: DEC-078, programa que elige el estudiante en un grupo compartido (opcional). */
    record Comando(String token, String pin, String direccionIp, UUID usuarioEjecutor, UUID programaId) {
        public Comando(final String token, final String pin, final String direccionIp, final UUID usuarioEjecutor) {
            this(token, pin, direccionIp, usuarioEjecutor, null);
        }
    }

    record Resultado(UUID idGrupo) {
    }
}
