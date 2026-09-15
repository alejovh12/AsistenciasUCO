package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import java.util.UUID;

/**
 * Resultado canonico devuelto por los procedimientos almacenados publicos.
 */
public final class CanonicalProcedureResult {

    private final UUID idCorrelacion;
    private final String mensajeUsuarioResultado;
    private final String mensajeTecnicoResultado;
    private final boolean estadoResultado;

    public CanonicalProcedureResult(
            final UUID idCorrelacion,
            final String mensajeUsuarioResultado,
            final String mensajeTecnicoResultado,
            final boolean estadoResultado
    ) {
        this.idCorrelacion = idCorrelacion;
        this.mensajeUsuarioResultado = mensajeUsuarioResultado;
        this.mensajeTecnicoResultado = mensajeTecnicoResultado;
        this.estadoResultado = estadoResultado;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public String getMensajeUsuarioResultado() {
        return mensajeUsuarioResultado;
    }

    public String getMensajeTecnicoResultado() {
        return mensajeTecnicoResultado;
    }

    public boolean isEstadoResultado() {
        return estadoResultado;
    }
}
