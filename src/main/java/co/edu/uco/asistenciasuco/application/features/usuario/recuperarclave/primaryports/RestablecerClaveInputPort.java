package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports;

/** HU177: puerto de entrada para definir la contrasena nueva con el token del enlace. */
public interface RestablecerClaveInputPort {

    /** @return el mensaje institucional de exito. */
    String execute(String token, String claveNueva);
}
