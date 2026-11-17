package co.edu.uco.asistenciasuco.application.primaryports;

/**
 * Contrato global para casos de uso que retornan resultado.
 *
 * @param <D> tipo de entrada
 * @param <R> tipo de salida
 */
public interface InteractorWithReturn<D, R> {

    R execute(D dto);
}
