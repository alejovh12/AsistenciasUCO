package co.edu.uco.notificaciones.domain;

import java.util.Objects;

/** Resultado del renderizado: asunto, texto plano, HTML con layout y origen de la plantilla usada. */
public record ContenidoRenderizado(String asunto, String textoPlano, String html, String origen) {

    public ContenidoRenderizado {
        Objects.requireNonNull(asunto, "El asunto renderizado es obligatorio.");
        Objects.requireNonNull(textoPlano, "El texto plano renderizado es obligatorio.");
        Objects.requireNonNull(html, "El HTML renderizado es obligatorio.");
        Objects.requireNonNull(origen, "El origen del renderizado es obligatorio.");
    }
}
