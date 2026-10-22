package co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta;

/**
 * Resultado de sincronizar la cuenta de identidad despues de un cambio ya confirmado en la base de datos.
 */
public enum ResultadoSincronizacion {
    /** La cuenta de identidad quedo alineada con la base de datos. */
    OK,
    /** El proveedor no pudo aplicar el cambio; queda registrado como desfase pendiente de reconciliar. */
    PENDIENTE
}
