package co.edu.uco.asistenciasuco.application.secondaryports.identity.projection;

import java.util.UUID;

/**
 * Vinculo institucional del usuario (HU006). rol: ESTUDIANTE, DOCENTE, COORDINADOR (entidad = programa), DECANO
 * (facultad) o ADMINISTRADOR (institucion). detalle: facultad del programa (tambien para DOCENTE desde MEJORAS-SERGIO-2026-11-18) o institucion.
 */
public record VinculoUsuarioProjection(
        String rol,
        UUID idEntidad,
        String nombreEntidad,
        String detalle,
        boolean estaActivo
) {
}
