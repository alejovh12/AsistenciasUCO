package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * HU104/HU105/HU109/HU119/HU145: consultas de solo lectura del decano sobre su facultad. El provider DB filtra por
 * el decano ejecutor (vistas {@code uv_auth_*}): un decano nunca ve datos de otra facultad.
 */
public interface FacultadDecanoQueryPort {

    Pagina<EstudianteFacultadProjection> consultarEstudiantes(UUID idUsuarioEjecutor, String busqueda, UUID idPrograma,
                                                              Boolean estaActivo, int pagina, int tamano);

    List<MatriculaEstudianteProjection> consultarDetalleEstudiante(UUID idUsuarioEjecutor, UUID idEstudiante);

    List<EstructuraFacultadProjection> consultarEstructura(UUID idUsuarioEjecutor);

    Pagina<SesionFacultadProjection> consultarSesiones(UUID idUsuarioEjecutor, LocalDate desde, LocalDate hasta,
                                                       UUID idPrograma, int pagina, int tamano);

    record Pagina<T>(List<T> elementos, int total) {
    }

    record EstudianteFacultadProjection(UUID idEstudiante, String numeroIdentificacion, String nombreCompleto,
                                        String correo, boolean activo, String programas, Integer totalGrupos) {
    }

    record MatriculaEstudianteProjection(UUID idEstudiante, String numeroIdentificacion, String nombreCompleto,
                                         UUID idPrograma, String nombrePrograma, Integer inpPlanEstudio,
                                         UUID idAsignatura, String nombreAsignatura, UUID idGrupo, String nombreGrupo,
                                         boolean activo) {
    }

    record EstructuraFacultadProjection(UUID idFacultad, String nombreFacultad, String nombreDecano,
                                        boolean facultadActiva, UUID idPrograma, String nombrePrograma,
                                        String nombreCoordinador, boolean programaActivo, String nombreTipoPrograma,
                                        String nombreModalidad, String nombreJornada, UUID idPlanEstudio,
                                        Integer inpPlanEstudio, boolean planActivo, Integer totalSemestres) {
    }

    record SesionFacultadProjection(UUID id, String nombre, String codigo, Integer numeroSemana,
                                    LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, UUID idGrupo,
                                    Integer codigoGrupo, String nombreGrupo, UUID idAsignatura,
                                    String nombreAsignatura, UUID idPrograma, String nombrePrograma) {
    }
}
