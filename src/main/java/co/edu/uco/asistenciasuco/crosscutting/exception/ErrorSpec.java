package co.edu.uco.asistenciasuco.crosscutting.exception;

/**
 * Valores de una definicion de error (codigo publico, texto de respaldo y tipo). Los enums de codigos de error la usan como
 * unico campo y delegan en ella, en lugar de repetir los tres campos, el constructor y los tres getters en cada enum.
 */
public record ErrorSpec(String code, String defaultMessage, ErrorKind kind) implements ErrorDefinition {
}
