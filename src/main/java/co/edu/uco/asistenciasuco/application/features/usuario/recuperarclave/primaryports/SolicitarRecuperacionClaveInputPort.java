package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports;

/** HU177: puerto de entrada de la solicitud publica del enlace de recuperacion. */
public interface SolicitarRecuperacionClaveInputPort {

    /** @return el mensaje institucional, igual exista o no la cuenta. */
    String execute(String correo);
}
