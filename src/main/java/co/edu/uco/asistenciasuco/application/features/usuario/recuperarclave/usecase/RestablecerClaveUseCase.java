package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase;

/** HU177: define la contrasena nueva con el token de un solo uso del enlace. */
public interface RestablecerClaveUseCase {

    /**
     * @return el mensaje institucional de exito.
     * @throws co.edu.uco.asistenciasuco.application.exception.validation.ValidationException con un unico codigo para cualquier
     *         enlace que no sirve, o con el codigo de la politica de contrasenas si la clave no la cumple.
     */
    String execute(String token, String claveNueva);
}
