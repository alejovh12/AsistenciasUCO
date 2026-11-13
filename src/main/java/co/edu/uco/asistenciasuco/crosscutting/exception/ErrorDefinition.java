package co.edu.uco.asistenciasuco.crosscutting.exception;

public interface ErrorDefinition {

    String code();

    String defaultMessage();

    ErrorKind kind();

    /**
     * Codigo con el que se busca el texto de usuario en el catalogo de mensajes (VAL-oleada3 MSG-05). Por defecto es el mismo
     * {@link #code()}; una definicion puede conservar el codigo publico de la respuesta (p. ej. {@code VALIDATION_ERROR}) y
     * resolver su texto con un codigo de catalogo propio.
     */
    default String messageCode() {
        return code();
    }
}
