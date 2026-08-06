package co.edu.uco.asistenciasuco.application.usecase;

/**
 * Contrato global para casos de uso sin entrada y con retorno.
 *
 * @param <RESULT> tipo de salida
 */
public interface UseCaseWithoutInputWithReturn<RESULT> {

    RESULT execute();
}
