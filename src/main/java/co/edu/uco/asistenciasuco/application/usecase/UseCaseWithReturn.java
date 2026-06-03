package co.edu.uco.asistenciasuco.application.usecase;

/**
 * Contrato global para casos de uso que retornan resultado.
 *
 * @param <DOMAIN> tipo de dominio de entrada
 * @param <RESULT> tipo de salida
 */
public interface UseCaseWithReturn<DOMAIN, RESULT> {

    RESULT execute(DOMAIN domain);
}
