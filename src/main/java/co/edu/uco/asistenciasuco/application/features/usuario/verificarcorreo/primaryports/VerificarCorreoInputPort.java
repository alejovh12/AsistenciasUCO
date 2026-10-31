package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports;

/** IDENT-001: puerto de entrada de la verificacion publica del correo. */
public interface VerificarCorreoInputPort {

    /** @return el mensaje institucional de exito. */
    String execute(String token);
}
