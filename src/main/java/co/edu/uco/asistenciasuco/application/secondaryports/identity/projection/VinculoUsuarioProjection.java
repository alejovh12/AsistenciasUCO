package co.edu.uco.asistenciasuco.application.secondaryports.identity.projection;

import java.util.UUID;

/**
 * Vinculo institucional del usuario (HU006). rol: ESTUDIANTE, DOCENTE, COORDINADOR (entidad = programa), DECANO
 * (facultad) o ADMINISTRADOR (institucion). detalle: facultad del programa, tipo de vinculacion docente o institucion.
 */
public record VinculoUsuarioProjection(
        String rol,
        UUID idEntidad,
        String nombreEntidad,
        String detalle,
        boolean estaActivo
) {
}
