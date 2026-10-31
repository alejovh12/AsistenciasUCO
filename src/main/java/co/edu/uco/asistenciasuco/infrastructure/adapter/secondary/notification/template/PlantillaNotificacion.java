package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import java.util.List;
import java.util.Objects;

/**
 * Plantilla activa de notificacion con sus marcadores declarados.
 */
public record PlantillaNotificacion(
        int version,
        String asunto,
        String cuerpo,
        List<Marcador> marcadores
) {

    public PlantillaNotificacion {
        if (version < 1) {
            throw new IllegalArgumentException("La version de la plantilla debe ser >= 1.");
        }
        Objects.requireNonNull(asunto, "El asunto de la plantilla es obligatorio.");
        Objects.requireNonNull(cuerpo, "El cuerpo de la plantilla es obligatorio.");
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }

    public record Marcador(String nombre, boolean obligatorio) {

        public Marcador {
            Objects.requireNonNull(nombre, "El nombre del marcador es obligatorio.");
        }
    }
}
