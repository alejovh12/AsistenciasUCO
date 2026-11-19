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
                                                       UUID idPrograma, UUID idGrupo, int pagina, int tamano);

    record Pagina<T>(List<T> elementos, int total) {
    }

    /** MEJORAS-SERGIO-2026-11-18 (item 5): {@code codigoTipoIdentificacion} es la sigla del tipo de documento (CC, TI...). */
    record EstudianteFacultadProjection(UUID idEstudiante, String numeroIdentificacion, String nombreCompleto,
                                        String correo, boolean activo, String programas, Integer totalGrupos,
                                        String codigoTipoIdentificacion) {
        public EstudianteFacultadProjection(final UUID idEstudiante, final String numeroIdentificacion, final String nombreCompleto,
                                            final String correo, final boolean activo, final String programas,
                                            final Integer totalGrupos) {
            this(idEstudiante, numeroIdentificacion, nombreCompleto, correo, activo, programas, totalGrupos, null);
        }
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
