package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import java.util.Objects;

/**
 * Resultado del renderizado de una notificacion (asunto, texto plano, HTML y origen).
 */
public record ContenidoRenderizado(
        String asunto,
        String textoPlano,
        String html,
        String origen
) {

    public ContenidoRenderizado {
        Objects.requireNonNull(asunto, "El asunto renderizado es obligatorio.");
        Objects.requireNonNull(textoPlano, "El texto plano renderizado es obligatorio.");
        Objects.requireNonNull(html, "El HTML renderizado es obligatorio.");
        Objects.requireNonNull(origen, "El origen del renderizado es obligatorio.");
    }
}
