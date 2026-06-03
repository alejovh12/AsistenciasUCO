package co.edu.uco.asistenciasuco.application.primaryports;

/**
 * Contrato global para casos de uso sin retorno.
 *
 * @param <DTO> tipo de entrada
 */
public interface InteractorWithoutReturn<DTO> {

    void execute(DTO dto);
}
