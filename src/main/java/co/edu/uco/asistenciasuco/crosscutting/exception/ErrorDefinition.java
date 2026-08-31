package co.edu.uco.asistenciasuco.crosscutting.exception;

public interface ErrorDefinition {

    String code();

    String defaultMessage();

    ErrorKind kind();
}
