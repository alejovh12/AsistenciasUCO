package co.edu.uco.asistenciasuco.application.secondaryports.security;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

/**
 * Puerto secundario neutral del perfil de acceso de una persona en el motor de autorización (PDP).
 *
 * <p>Asignar un rol en el proveedor de identidad NO concede acceso: el motor de autorización decide por los perfiles
 * que tenga la persona. Este puerto asegura, de forma idempotente, que el perfil equivalente a un rol institucional
 * esté (o no) asignado. Nunca lanza: cualquier falla del motor se devuelve como estado pendiente o rechazado, y
 * mientras el perfil no esté sincronizado la persona no tiene acceso (fail-closed).</p>
 *
 * <p>La persona se identifica por su correo verificado del proveedor de identidad; el vínculo autoritativo
 * {@code issuer + subject → Usuario.id} vive en la base institucional y no se sustituye por el correo.</p>
 */
public interface AccessProfilePort {

    /** Asegura que el perfil del rol esté asignado. Idempotente. */
    Resultado sincronizar(String correo, InstitutionalRole rol);

    /** Asegura que el perfil del rol NO esté asignado (cierra su vigencia). Idempotente. */
    Resultado revocar(String correo, InstitutionalRole rol);

    /** Consulta el estado actual sin modificar nada. */
    Resultado consultar(String correo, InstitutionalRole rol);

    enum Estado {
        /** El perfil está asignado y vigente. */
        ASIGNADO,
        /** El perfil no está asignado (o ya fue revocado). */
        NO_ASIGNADO,
        /** El motor aún no conoce a la persona (se registra en su primer acceso): no hay acceso hasta sincronizar. */
        PENDIENTE_PERSONA_NO_REGISTRADA,
        /** El motor no respondió o no aceptó la credencial de servicio: no hay acceso hasta reconciliar. */
        PENDIENTE_NO_DISPONIBLE,
        /** El motor rechazó la operación de forma controlada (p. ej. conflicto); requiere revisión. */
        RECHAZADO,
        /** El proveedor de seguridad activo no usa perfiles externos. */
        NO_APLICA
    }

    /** @param detalle texto técnico sin datos sensibles. */
    record Resultado(Estado estado, String detalle) {
    }
}
