package co.edu.uco.asistenciasuco.application.usecase;

/**
 * Contrato global para casos de uso sin retorno.
 *
 * @param <DOMAIN> tipo de dominio de entrada
 */
public interface UseCaseWithoutReturn<DOMAIN> {

    void execute(DOMAIN domain);
}
