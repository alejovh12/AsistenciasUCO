package co.edu.uco.asistenciasuco.application.secondaryports.notification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso institucional independiente del canal (correo, WhatsApp, SMS...): el componente de notificaciones decide
 * como entregarlo a cada destinatario. {@code datos} lleva identificadores para trazabilidad, nunca secretos.
 */
public record Notificacion(
        String tipo,
        String asunto,
        String mensaje,
        List<Destinatario> destinatarios,
        Map<String, String> datos
) {

    /** HU051: el docente titular retira a un estudiante del grupo. */
    public static final String TIPO_RETIRO_ESTUDIANTE_GRUPO = "RETIRO_ESTUDIANTE_GRUPO";

    public Notificacion {
        Objects.requireNonNull(tipo, "El tipo de notificacion es obligatorio.");
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    /** Destinatario con su rol en el evento (ESTUDIANTE, DOCENTE, COORDINADOR...) y sus datos de contacto. */
    public record Destinatario(String rol, UUID idUsuario, String nombre, String correo) {
    }
}
