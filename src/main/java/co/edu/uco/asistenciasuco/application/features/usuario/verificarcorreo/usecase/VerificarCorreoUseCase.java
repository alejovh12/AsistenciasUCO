package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase;

/** IDENT-001: verifica el correo de un usuario con el token de un solo uso recibido por correo. */
public interface VerificarCorreoUseCase {

    /**
     * @return el mensaje institucional de exito que se muestra al usuario.
     * @throws co.edu.uco.asistenciasuco.application.exception.validation.ValidationException con el mismo mensaje para
     *         cualquier token invalido.
     */
    String execute(String token);
}
