package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record ParametroProjection(
        UUID id,
        String grupo,
        String clave,
        String valor,
        String tipoDato,
        String valorDefecto,
        boolean estaActivo
) {
}
