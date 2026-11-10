package co.edu.uco.asistenciasuco.application.features.sesionmanual.common;

import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 y DEC-065: piezas comunes de los avisos de sesiones extraordinarias. La DB guarda la sesion en UTC; los textos
 * del aviso se muestran en la hora institucional (America/Bogota). Un aviso agrupado es UNA {@link Notificacion} con todos los
 * destinatarios del mismo rol (el canal los reparte en bloques BCC), por lo que el saludo es colectivo.
 */
public final class AvisoSesionSupport {

    public static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");
    /** Saludo de un aviso agrupado a los estudiantes del grupo. */
    public static final String SALUDO_ESTUDIANTES = "estudiantes del grupo";
    /** Saludo de un aviso agrupado a los estudiantes y al docente titular del grupo. */
    public static final String SALUDO_ESTUDIANTES_Y_DOCENTE = "estudiantes y docente del grupo";
    /** Saludo de un aviso agrupado a los coordinadores del programa. */
    public static final String SALUDO_COORDINADORES = "coordinación del programa";
    /** Quien decide una solicitud cuando el aviso no identifica a una persona concreta. */
    public static final String COORDINADOR_DEL_PROGRAMA = "El coordinador del programa";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private AvisoSesionSupport() {
    }

    public static String fecha(final LocalDateTime utc) {
        return formatear(utc, FORMATO_FECHA);
    }

    public static String hora(final LocalDateTime utc) {
        return formatear(utc, FORMATO_HORA);
    }

    public static String valor(final String texto) {
        return texto == null ? "" : texto;
    }

    /** Destinatarios de un mismo rol, en el orden recibido. */
    public static List<DestinatarioSesionManual> deTipo(final List<DestinatarioSesionManual> todos, final String tipo) {
        return todos.stream().filter(d -> tipo.equals(d.tipo())).toList();
    }

    public static List<Notificacion.Destinatario> comoDestinatarios(final List<DestinatarioSesionManual> destinatarios) {
        return destinatarios.stream()
                .map(d -> new Notificacion.Destinatario(d.tipo(), d.idUsuario(), d.nombreCompleto(), d.correo()))
                .toList();
    }

    /** Datos comunes a todas las plantillas de sesion; la referencia aporta asignatura, grupo, docente y horario de la sesion. */
    public static Map<String, String> datosBase(final UUID sesionId, final DestinatarioSesionManual referencia) {
        Objects.requireNonNull(referencia, "El destinatario de referencia es obligatorio.");
        final Map<String, String> datos = new HashMap<>();
        datos.put("asignatura", valor(referencia.nombreAsignatura()));
        datos.put("grupo", valor(referencia.nombreGrupo()));
        datos.put("fecha", fecha(referencia.fechaHoraInicio()));
        datos.put("horaInicio", hora(referencia.fechaHoraInicio()));
        datos.put("horaFin", hora(referencia.fechaHoraFin()));
        datos.put("docente", valor(referencia.nombreDocente()));
        datos.put("idSesion", sesionId.toString());
        return datos;
    }

    /** " del grupo G para el dd/MM/yyyy de HH:mm a HH:mm." segun la sesion de referencia. */
    public static String ventana(final Map<String, String> datos) {
        return " del grupo " + datos.get("grupo") + " para el " + datos.get("fecha") + " de " + datos.get("horaInicio") + " a "
                + datos.get("horaFin") + ".";
    }

    /** ISO-8601 con {@code Z} solo si el horario esta confirmado en UTC (v2); si no, el valor local sin zona de v1. */
    public static String fechaHoraInicio(final SesionProcedenciaRepositoryProjection sesion) {
        final boolean confirmada = EstadoHorarioSesion.desdeProcedencia(sesion.procedenciaTemporal()) == EstadoHorarioSesion.CONFIRMADA;
        return confirmada ? sesion.fechaHoraInicio().toInstant(ZoneOffset.UTC).toString() : sesion.fechaHoraInicio().toString();
    }

    /** Una sola notificacion a todos los estudiantes (o a todos los coordinadores) con el horario de la primera fila. */
    public static Optional<Notificacion> avisoSesionCreada(final UUID sesionId, final List<DestinatarioSesionManual> grupo,
                                                           final boolean paraEstudiantes) {
        if (grupo.isEmpty()) {
            return Optional.empty();
        }
        final Map<String, String> datos = datosBase(sesionId, grupo.getFirst());
        final String asignatura = datos.get("asignatura");
        final String fecha = datos.get("fecha");
        final String docente = datos.get("docente");
        final String saludo = paraEstudiantes ? SALUDO_ESTUDIANTES : SALUDO_COORDINADORES;
        datos.put(paraEstudiantes ? "estudiante" : "coordinador", saludo);
        final String ventana = ventana(datos);
        final String asunto;
        final String mensaje;
        if (paraEstudiantes) {
            asunto = "Nueva sesión de clase: " + asignatura + " (" + fecha + ")";
            mensaje = "Hola " + saludo + ". El docente " + docente + " programó una sesión adicional de " + asignatura + ventana;
        } else {
            asunto = "Sesión creada manualmente: " + asignatura + " (" + fecha + ")";
            mensaje = "Hola " + saludo + ". El docente " + docente + " creó manualmente una sesión de " + asignatura + ventana
                    + " Si no corresponde, puede cancelarla desde su vista de sesiones.";
        }
        return Optional.of(new Notificacion(
                paraEstudiantes ? Notificacion.TIPO_SESION_MANUAL_CREADA_ESTUDIANTE : Notificacion.TIPO_SESION_MANUAL_CREADA_COORDINADOR,
                asunto, mensaje, comoDestinatarios(grupo), datos));
    }

    private static String formatear(final LocalDateTime utc, final DateTimeFormatter formato) {
        return utc == null ? "" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONA_INSTITUCIONAL).format(formato);
    }
}
