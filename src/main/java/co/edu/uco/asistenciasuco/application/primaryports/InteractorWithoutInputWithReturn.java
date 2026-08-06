package co.edu.uco.asistenciasuco.application.primaryports;

/**
 * Contrato global para interactores sin entrada y con retorno.
 *
 * @param <RESULT> tipo de salida
 */
public interface InteractorWithoutInputWithReturn<RESULT> {

    RESULT execute();
}
