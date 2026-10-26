package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

/** No se permite elegir arbitrariamente una fila al vincular una identidad externa. */
public final class InstitutionalIdentityAmbiguityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InstitutionalIdentityAmbiguityException() {
        super("La identidad institucional coincide con multiples usuarios.");
    }
}
