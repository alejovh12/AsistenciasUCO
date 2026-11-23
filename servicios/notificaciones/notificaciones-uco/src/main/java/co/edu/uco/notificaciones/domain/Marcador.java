package co.edu.uco.notificaciones.domain;

/** Marcador permitido en las plantillas de un tipo: {@code {nombre}}. */
public record Marcador(String marcador, String descripcion, String ejemplo, boolean obligatorio) {
}
