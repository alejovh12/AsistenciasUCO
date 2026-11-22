package co.edu.uco.asistenciasuco.application.exception.catalog;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorSpec;
import co.edu.uco.asistenciasuco.crosscutting.exception.SpecErrorDefinition;

/**
 * Codigos de error por dependencia externa caida (REA-02, ver {@code docs/architecture/degradacion-por-dependencia.md}). Coinciden
 * con el catalogo de mensajes de la BD (migracion 67); el texto de aqui es la contingencia ante desconexion del catalogo, que con
 * SQL Server caido es precisamente el caso normal.
 */
public enum DependenciaErrorCode implements SpecErrorDefinition {
    /**
     * REA-02 (revision final P3): SQL Server no acepta conexiones (rechazada, sin ruta, pool agotado por timeout de conexion). Se
     * publica como 503 y no como 500 generico. Un error ordinario de SQL (restriccion, rechazo de negocio de un SP) no usa este
     * codigo.
     */
    ERR_BD_NO_DISPONIBLE(new ErrorSpec("ERR_BD_NO_DISPONIBLE",
            "El servicio no está disponible en este momento. Intenta de nuevo en unos minutos.",
            ErrorKind.SERVICE_UNAVAILABLE)),
    /**
     * P3 F5 (migracion 71, seed 74 de BD): el Servicio de Notificaciones no responde, agota el tiempo, contesta 5xx, 408 o 429, rechaza
     * la credencial de administracion o su circuit breaker esta abierto. Los avisos pendientes siguen en la outbox y se reintentan.
     */
    ERR_NOTIFICACIONES_NO_DISPONIBLE(new ErrorSpec("ERR_NOTIFICACIONES_NO_DISPONIBLE",
            "El servicio de notificaciones no está disponible en este momento. Intenta de nuevo en unos minutos.",
            ErrorKind.SERVICE_UNAVAILABLE));

    private final ErrorSpec spec;

    DependenciaErrorCode(final ErrorSpec spec) {
        this.spec = spec;
    }

    @Override
    public ErrorSpec spec() {
        return spec;
    }
}
