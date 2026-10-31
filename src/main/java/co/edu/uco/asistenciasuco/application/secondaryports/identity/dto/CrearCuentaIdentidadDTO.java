package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.UUID;

/**
 * Datos necesarios para crear una cuenta institucional en el proveedor de identidad.
 * Este DTO es agnóstico al IdP y no contiene referencias a tecnologías concretas.
 *
 * @param username                  Nombre de usuario institucional (ej: número de identificación).
 * @param idUsuario                 Identificador interno del usuario en la DB institucional.
 * @param correo                    Correo electrónico institucional.
 * @param primerNombre              Primer nombre del usuario.
 * @param primerApellido            Primer apellido del usuario.
 * @param passwordInicial           Contraseña inicial asignada por el sistema. Solo se aplica si el
 *                                  IdP determina que la cuenta es nueva; nunca se usa para resetear
 *                                  la contraseña de una cuenta ya existente.
 * @param rolInstitucional          Rol institucional a asignar en el IdP. El adapter traduce este enum
 *                                  al nombre exacto del client role en el IdP (ej. {@code name()}) —
 *                                  Application nunca envía nombres técnicos, alias, ni prefijos como
 *                                  {@code ROLE_}.
 * @param requiereVerificacionCorreo IDENT-001: si es {@code true} y la cuenta es nueva en el IdP, se crea
 *                                  deshabilitada y con el correo sin verificar hasta que el titular lo confirme
 *                                  ({@link co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort#confirmarCorreoCuenta}).
 *                                  Una cuenta que ya existía en el IdP nunca se modifica.
 */
public record CrearCuentaIdentidadDTO(
        String username,
        UUID idUsuario,
        String correo,
        String primerNombre,
        String primerApellido,
        String passwordInicial,
        InstitutionalRole rolInstitucional,
        boolean requiereVerificacionCorreo
) {

    /** Compatibilidad: sin verificación pendiente (cuenta habilitada y correo confiable), como antes de IDENT-001. */
    public CrearCuentaIdentidadDTO(
            final String username,
            final UUID idUsuario,
            final String correo,
            final String primerNombre,
            final String primerApellido,
            final String passwordInicial,
            final InstitutionalRole rolInstitucional
    ) {
        this(username, idUsuario, correo, primerNombre, primerApellido, passwordInicial, rolInstitucional, false);
    }

    /** Copia del DTO que exige la verificación del correo antes de habilitar la cuenta nueva. */
    public CrearCuentaIdentidadDTO conVerificacionCorreo() {
        return new CrearCuentaIdentidadDTO(
                username, idUsuario, correo, primerNombre, primerApellido, passwordInicial, rolInstitucional, true);
    }
}
