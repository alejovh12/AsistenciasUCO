package co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pagina de la matriz de asistencia: encabezado con las sesiones del grupo (orden por numero), una fila por estudiante de la
 * pagina con una celda por sesion y el total de estudiantes que cumplen la busqueda.
 */
public record MatrizAsistenciaGrupoDTO(List<Sesion> sesiones, List<Fila> filas, int numeroPagina, int tamanoPagina,
                                       long totalElementos) {

    public MatrizAsistenciaGrupoDTO {
        sesiones = sesiones == null ? List.of() : List.copyOf(sesiones);
        filas = filas == null ? List.of() : List.copyOf(filas);
    }

    /** Sesion del encabezado; {@code estadoSesion}: PROGRAMADA, EN_CURSO, EJECUTADA o CANCELADA. */
    public record Sesion(UUID idSesion, int numero, String nombre, String estadoSesion, Instant fechaHoraInicioUtc,
                         Instant fechaHoraFinUtc, boolean horarioConfirmado) {
    }

    public record Fila(UUID idEstudianteGrupo, UUID idEstudiante, String nombreCompleto, String documento,
                       String codigoEstadoEstudiante, List<Celda> celdas) {

        public Fila {
            celdas = celdas == null ? List.of() : List.copyOf(celdas);
        }
    }

    /** {@code codigoEstado}: PRESENTE, PARCIAL, JUSTIFICADA, AUSENTE, SIN_REGISTRO o NO_DISPONIBLE. */
    public record Celda(UUID idSesion, String codigoEstado, int minutosAsistidos, int minutosSesion) {
    }
}
