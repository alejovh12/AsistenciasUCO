package co.edu.uco.asistenciasuco.application.primaryports;

/**
 * Contrato global para casos de uso que retornan resultado.
 *
 * @param <DTO> tipo de entrada
 * @param <RESULT> tipo de salida
 */
public interface InteractorWithReturn<DTO, RESULT> {

    RESULT execute(DTO dto);
}
