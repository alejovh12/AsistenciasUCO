package co.edu.uco.asistenciasuco.application.primaryports;

/**
 * Contrato global para interactores sin entrada y con retorno.
 *
 * @param <R> tipo de salida
 */
public interface InteractorWithoutInputWithReturn<R> {

    R execute();
}
