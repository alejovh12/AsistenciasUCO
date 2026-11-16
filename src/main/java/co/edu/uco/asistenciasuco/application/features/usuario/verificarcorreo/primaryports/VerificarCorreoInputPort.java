package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports;

/** IDENT-001: puerto de entrada de la verificacion publica del correo. */
public interface VerificarCorreoInputPort {

    /**
     * @param contrasena IDENT-002: contrasena que define el titular al activar una cuenta registrada por otra persona;
     *                   se ignora si la cuenta ya tiene una.
     * @return el mensaje institucional de exito.
     */
    String execute(String token, String contrasena);
}
