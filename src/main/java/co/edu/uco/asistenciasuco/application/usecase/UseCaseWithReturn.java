package co.edu.uco.asistenciasuco.application.usecase;

/**
 * Contrato global para casos de uso que retornan resultado.
 *
 * @param <D> tipo de dominio de entrada
 * @param <R> tipo de salida
 */
public interface UseCaseWithReturn<D, R> {

    R execute(D domain);
}
