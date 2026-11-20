package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** HU176B (CA-03): alta PUBLICA de un estudiante sin cuenta y su matricula con un token o PIN vigente. */
public interface RegistrarEstudiantePorTokenInputPort
        extends InteractorWithReturn<RegistrarEstudiantePorTokenInputPort.Comando, RegistrarEstudiantePorTokenInputPort.Resultado> {

    record Comando(
            String token,
            String pin,
            String direccionIp,
            UUID tipoIdentificacionId,
            Integer numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            String password,
            UUID programaId
    ) {
        /** Sin programa elegido (DEC-078): el de la ventana o el unico del grupo. */
        public Comando(final String token, final String pin, final String direccionIp, final UUID tipoIdentificacionId,
                       final Integer numeroIdentificacion, final String primerNombre, final String segundoNombre,
                       final String primerApellido, final String segundoApellido, final String correo, final String password) {
            this(token, pin, direccionIp, tipoIdentificacionId, numeroIdentificacion, primerNombre, segundoNombre,
                    primerApellido, segundoApellido, correo, password, null);
        }
    }

    /** {@code usuario} es el numero de identificacion con el que inicia sesion. */
    record Resultado(UUID idGrupo, String usuario) {
    }
}
