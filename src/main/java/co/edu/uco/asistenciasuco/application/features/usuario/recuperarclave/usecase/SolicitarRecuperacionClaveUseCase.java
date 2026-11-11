package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase;

/** HU177: pide el enlace de un solo uso para definir una contrasena nueva. */
public interface SolicitarRecuperacionClaveUseCase {

    /**
     * @return el mismo mensaje institucional exista o no una cuenta con ese correo, para no revelar quien esta registrado.
     */
    String execute(String correo);
}
