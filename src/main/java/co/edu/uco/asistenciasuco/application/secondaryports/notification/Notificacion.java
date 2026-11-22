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
    /**
     * IDENT-002: aviso a una persona registrada por otra (decano, coordinador, docente o estudiante) para activar su cuenta
     * y definir su contrasena. Usa el mismo token de un solo uso que {@link #TIPO_VERIFICACION_CORREO}.
     */
    public static final String TIPO_ACTIVACION_CUENTA = "ACTIVACION_CUENTA";
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

    /** CULM-001 / DEC-064 (HU047): el docente creo una sesion manualmente (aviso a cada estudiante activo del grupo). */
    public static final String TIPO_SESION_MANUAL_CREADA_ESTUDIANTE = "SESION_MANUAL_CREADA_ESTUDIANTE";
    /** CULM-001 / DEC-064 (HU047): el docente creo una sesion manualmente (aviso al coordinador del programa). */
    public static final String TIPO_SESION_MANUAL_CREADA_COORDINADOR = "SESION_MANUAL_CREADA_COORDINADOR";
    /** DEC-065 (2): el docente solicito una sesion extraordinaria (un solo aviso agrupado a los coordinadores del programa). */
    public static final String TIPO_SESION_SOLICITUD_CREADA_COORDINADOR = "SESION_SOLICITUD_CREADA_COORDINADOR";
    /** DEC-065 (2): el coordinador aprobo la solicitud (aviso al docente titular). */
    public static final String TIPO_SESION_SOLICITUD_APROBADA_DOCENTE = "SESION_SOLICITUD_APROBADA_DOCENTE";
    /** DEC-065 (2): el coordinador rechazo la solicitud (aviso al docente titular, con motivo y observacion). */
    public static final String TIPO_SESION_SOLICITUD_RECHAZADA_DOCENTE = "SESION_SOLICITUD_RECHAZADA_DOCENTE";

    /** ATA D1 (HU175): aviso al estudiante de las tomas en falta de una sesion que ya termino (un correo por estudiante y sesion). */
    public static final String TIPO_FALTA_ASISTENCIA_SESION_ESTUDIANTE = "FALTA_ASISTENCIA_SESION_ESTUDIANTE";
    /** NOTOMA-001: aviso al coordinador de que el docente no tomo asistencia en una sesion que ya termino (un correo por coordinador y sesion). */
    public static final String TIPO_SESION_SIN_TOMA_COORDINADOR = "SESION_SIN_TOMA_COORDINADOR";

    /** DEC-081: un dia de receso academico cancelo clases del estudiante (un correo por persona con el resumen de sus clases). */
    public static final String TIPO_RECESO_CLASES_CANCELADAS_ESTUDIANTE = "RECESO_CLASES_CANCELADAS_ESTUDIANTE";
    /** DEC-081: un dia de receso academico cancelo clases del docente titular (un correo por persona con el resumen de sus clases). */
    public static final String TIPO_RECESO_CLASES_CANCELADAS_DOCENTE = "RECESO_CLASES_CANCELADAS_DOCENTE";

    public Notificacion {
        Objects.requireNonNull(tipo, "El tipo de notificacion es obligatorio.");
        destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        datos = datos == null ? Map.of() : Map.copyOf(datos);
    }

    /** Destinatario con su rol en el evento (ESTUDIANTE, DOCENTE, COORDINADOR...) y sus datos de contacto. */
    public record Destinatario(String rol, UUID idUsuario, String nombre, String correo) {
    }
}
