package co.edu.uco.asistenciasuco.application.secondaryports.identity;

import java.util.UUID;

/**
 * Puerto secundario para dejar constancia de un desfase entre la base de datos (fuente de verdad) y el
 * proveedor de identidad: el cambio ya se confirmo en DB pero la cuenta de identidad no pudo sincronizarse.
 *
 * <p>Nunca debe lanzar al llamador: registrar el desfase no puede ocultar ni revertir la operacion principal.
 * La correlacion de la solicitud la resuelve el adapter desde su contexto: Application no la transporta.</p>
 */
public interface UsuarioSincronizacionIdentidadPort {

    /**
     * @param idUsuario     usuario cuya cuenta quedo desincronizada (puede ser nulo si no se conoce).
     * @param operacion     operacion que no pudo completarse: {@code DESACTIVAR}, {@code REACTIVAR} o {@code ACTUALIZAR}.
     * @param errorTecnico  detalle tecnico sin datos sensibles.
     */
    void registrar(UUID idUsuario, String operacion, String errorTecnico);
}
