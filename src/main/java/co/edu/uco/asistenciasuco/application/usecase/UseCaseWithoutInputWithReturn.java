package co.edu.uco.asistenciasuco.application.usecase;

/**
 * Contrato global para casos de uso sin entrada y con retorno.
 *
 * @param <R> tipo de salida
 */
public interface UseCaseWithoutInputWithReturn<R> {

    R execute();
}
