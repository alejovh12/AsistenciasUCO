package co.edu.uco.asistenciasuco.crosscutting.exception;

/**
 * Definicion de error que guarda sus valores en un {@link ErrorSpec} y delega en el. Los enums de codigos de error solo
 * exponen su {@link #spec()}; asi no repiten el constructor ni los tres getters en cada enum.
 */
public interface SpecErrorDefinition extends ErrorDefinition {

    ErrorSpec spec();

    @Override
    default String code() {
        return spec().code();
    }

    @Override
    default String defaultMessage() {
        return spec().defaultMessage();
    }

    @Override
    default ErrorKind kind() {
        return spec().kind();
    }
}
