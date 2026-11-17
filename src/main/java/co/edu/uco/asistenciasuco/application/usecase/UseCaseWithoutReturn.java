package co.edu.uco.asistenciasuco.application.usecase;

/**
 * Contrato global para casos de uso sin retorno.
 *
 * @param <D> tipo de dominio de entrada
 */
public interface UseCaseWithoutReturn<D> {

    void execute(D domain);
}
