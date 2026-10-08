package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import java.util.UUID;

/**
 * Salida canonica de los procedimientos almacenados publicos (LB-008 JPA-01). Vive solo en Infrastructure:
 * no es una entidad JPA ni un tipo de Application o Domain.
 */
public interface ProcedureResult {

    UUID getIdCorrelacion();

    String getMensajeUsuarioResultado();

    String getMensajeTecnicoResultado();

    boolean getEstadoResultado();
}
