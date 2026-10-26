package co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto;

/** Estado de acceso de una cuenta institucional, visible en la UI. */
public enum EstadoAccesoCuenta {
    /** Base institucional, proveedor de identidad y motor de autorización coinciden: la persona puede operar. */
    ACTIVA,
    /** Falta sincronizar el proveedor o el motor de autorización; no hay acceso hasta reconciliar. */
    PENDIENTE_SINCRONIZACION,
    /** Sin roles institucionales activos: acceso retirado y cuenta deshabilitada. */
    INACTIVA,
    /** Problema controlado que requiere revisión (conflicto, cuenta ausente). */
    PROBLEMA
}
