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
    /** IDENT-001: aviso al estudiante registrado por el docente para verificar su correo (el enlace lo emite el canal al entregar). */
    public static final String TIPO_VERIFICACION_CORREO = "VERIFICACION_CORREO";
    /** HU010/HU035: el estudiante radica una solicitud de revision de asistencia (aviso al docente titular). */
    public static final String TIPO_RECLAMO_ASISTENCIA_RADICADO = "RECLAMO_ASISTENCIA_RADICADO";
    /** HU037/HU042/HU146: el docente resuelve la solicitud de revision (aviso al estudiante). */
    public static final String TIPO_RECLAMO_ASISTENCIA_RESUELTO = "RECLAMO_ASISTENCIA_RESUELTO";
    /** HU012/HU015: alerta de riesgo de inasistencia (regla institucional 20%). */
    public static final String TIPO_ALERTA_RIESGO_INASISTENCIA = "ALERTA_RIESGO_INASISTENCIA";
    /** HU013: alerta de perdida de asignatura por inasistencia. */
    public static final String TIPO_ALERTA_PERDIDA_INASISTENCIA = "ALERTA_PERDIDA_INASISTENCIA";
    /** HU174: el administrador institucional ejecuta el cierre masivo del periodo academico. */
    public static final String TIPO_CIERRE_MASIVO_PERIODO_EJECUTADO = "CIERRE_MASIVO_PERIODO_EJECUTADO";
    /** CULM-001 B.1 (HU047): el docente titular cancelo una sesion (aviso a cada estudiante activo del grupo). */
    public static final String TIPO_SESION_CANCELADA = "SESION_CANCELADA";
    /** CULM-001 C.1 (HU026C): se creo la revision de cancelacion por inasistencia (aviso al docente titular). */
    public static final String TIPO_REVISION_CANCELACION_PENDIENTE = "REVISION_CANCELACION_PENDIENTE";
    /** CULM-001 C.1: el docente propone cancelar (aviso al coordinador o, por escalamiento, al decano). */
    public static final String TIPO_PROPUESTA_CANCELACION = "PROPUESTA_CANCELACION";
    /** CULM-001 C.1: el resolutor aprobo la cancelacion (aviso al estudiante y al docente). */
    public static final String TIPO_CANCELACION_APROBADA = "CANCELACION_APROBADA";
    /** CULM-001 C.1: el resolutor rechazo la cancelacion (aviso al docente). */
    public static final String TIPO_CANCELACION_RECHAZADA = "CANCELACION_RECHAZADA";

    public Notificacion {
        Objects.requireNonNull(tipo, "El tipo de notificacion es obligatorio.");
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    /** Destinatario con su rol en el evento (ESTUDIANTE, DOCENTE, COORDINADOR...) y sus datos de contacto. */
    public record Destinatario(String rol, UUID idUsuario, String nombre, String correo) {
    }
}
