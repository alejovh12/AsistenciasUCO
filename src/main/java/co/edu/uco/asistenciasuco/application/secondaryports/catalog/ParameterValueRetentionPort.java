package co.edu.uco.asistenciasuco.application.secondaryports.catalog;

/**
 * INT-004: retiene el ultimo valor valido de un parametro cuando la BD rechazo el valor que trae el Catalogo de Parametros externo
 * (por ejemplo una lista CIDR mal formada). La cache del proveedor sigue trayendo el valor rechazado del Catalogo; este puerto evita que
 * se sirva: mientras el Catalogo siga devolviendo exactamente el valor rechazado, la lectura devuelve el valor valido retenido.
 */
public interface ParameterValueRetentionPort {

    /**
     * Retiene el valor valido de un parametro cuyo valor externo la BD rechazo.
     *
     * @param grupo          grupo del parametro.
     * @param clave          clave del parametro.
     * @param valorRechazado valor que el Catalogo externo devuelve y que la BD rechazo; no se debe servir.
     * @param valorVigente   ultimo valor valido (el que guarda la BD); nulo si no hay ninguno (la lectura queda sin valor y aplica el
     *                       valor por defecto del codigo).
     */
    void retenerValorValido(String grupo, String clave, String valorRechazado, String valorVigente);

    /** El Catalogo ya trae un valor que la BD acepto: deja de retener. */
    void liberar(String grupo, String clave);
}
