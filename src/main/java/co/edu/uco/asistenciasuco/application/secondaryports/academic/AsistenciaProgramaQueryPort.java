package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * HU064/HU060/HU061: consultas de solo lectura del coordinador sobre su programa. El provider DB filtra por el
 * usuario ejecutor (vistas {@code uv_auth_*}): un coordinador nunca ve grupos, sesiones ni asistencias de otro
 * programa.
 */
public interface AsistenciaProgramaQueryPort {

    Pagina<GrupoProgramaProjection> consultarGrupos(UUID idUsuarioEjecutor, UUID idPeriodoAcademico, String busqueda,
                                                     int pagina, int tamano);

    Pagina<SesionGrupoProjection> consultarSesionesGrupo(UUID idUsuarioEjecutor, UUID idGrupo, LocalDate desde,
                                                          LocalDate hasta, int pagina, int tamano);

    Pagina<AsistenciaSesionProjection> consultarAsistenciasSesion(UUID idUsuarioEjecutor, UUID idSesion, Boolean asistio,
                                                                   int pagina, int tamano);

    record Pagina<T>(List<T> elementos, int total) {
    }

    record GrupoProgramaProjection(UUID id, Integer codigo, String nombre, UUID idAsignatura, String nombreAsignatura,
                                   UUID idDocente, Integer capacidadMaxima, Integer estudiantesActivos,
                                   Integer cuposDisponibles, boolean habilitado, LocalDate inicioPeriodo,
                                   LocalDate finPeriodo) {
    }

    record SesionGrupoProjection(UUID id, UUID idGrupo, Integer codigoGrupo, String nombreGrupo, String nombre,
                                 Integer numero, String codigo, Integer numeroSemana, LocalDateTime fechaHoraInicio,
                                 LocalDateTime fechaHoraFin) {
    }

    record AsistenciaSesionProjection(UUID idDetalleAsistencia, UUID idSesion, String nombreSesion, UUID idEstudiante,
                                      String numeroIdentificacion, String nombreCompleto, boolean asistio,
                                      String codigoRazonCausa, String nombreRazonCausa, LocalDateTime fechaHoraInicio,
                                      LocalDateTime fechaHoraFin) {
    }
}
