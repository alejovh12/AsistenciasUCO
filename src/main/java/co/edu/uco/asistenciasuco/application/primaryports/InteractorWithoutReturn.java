package co.edu.uco.asistenciasuco.application.primaryports;

/**
 * Contrato global para casos de uso sin retorno.
 *
 * @param <D> tipo de entrada
 */
public interface InteractorWithoutReturn<D> {

    void execute(D dto);
}
