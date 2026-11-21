package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Puerto secundario de la matricula por QR/PIN rotativo (HU176B). El token llega en claro como valor opaco: el adaptador
 * calcula su SHA-256 y la base de datos solo conoce el hash. Los rechazos de negocio (MQR_*) se propagan como
 * excepciones de aplicacion clasificadas por el adaptador.
 */
public interface MatriculaQrRepositoryPort {

    /** Exactamente uno de los dos valores debe venir informado. */
    record CredencialMatricula(String token, String pin) {
    }

    record VentanaAbierta(UUID idVentana, Instant fechaExpiracionUtc) {
    }

    record VentanaRotada(UUID idVentana, UUID idDocenteEmisor, Instant fechaRotacionUtc, Instant fechaExpiracionUtc) {
    }

    record VentanaPorRotar(UUID idGrupo, UUID idDocenteEmisor) {
    }

    /** DEC-078: programa del grupo que el estudiante puede elegir al matricularse por QR en un grupo compartido. */
    record ProgramaElegible(UUID idPrograma, String nombre) {
    }

    /**
     * {@code programasElegibles}: DEC-078. Vacio si el grupo tiene un solo programa o si el docente fijo el programa de la
     * ventana; si no, los programas del grupo entre los que elige el estudiante nuevo (o el que aun no pertenece a ninguno).
     */
    record CursoMatricula(
            UUID idGrupo,
            String nombreAsignatura,
            String nombreGrupo,
            String nombreDocente,
            Instant fechaExpiracionUtc,
            int cupoDisponible,
            List<ProgramaElegible> programasElegibles
    ) {
        public CursoMatricula {
            programasElegibles = programasElegibles == null ? List.of() : List.copyOf(programasElegibles);
        }

        public CursoMatricula(final UUID idGrupo, final String nombreAsignatura, final String nombreGrupo,
                              final String nombreDocente, final Instant fechaExpiracionUtc, final int cupoDisponible) {
            this(idGrupo, nombreAsignatura, nombreGrupo, nombreDocente, fechaExpiracionUtc, cupoDisponible, List.of());
        }
    }

    record MatriculaRealizada(UUID idEstudiante, UUID idUsuario, UUID idGrupo) {
    }

    record RegistroPublico(
            CredencialMatricula credencial,
            UUID tipoIdentificacionId,
            int numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            UUID programaId
    ) {
        /** Sin programa elegido: el de la ventana o, en un grupo de un solo programa, ese programa (lo resuelve la BD). */
        public RegistroPublico(final CredencialMatricula credencial, final UUID tipoIdentificacionId,
                               final int numeroIdentificacion, final String primerNombre, final String segundoNombre,
                               final String primerApellido, final String segundoApellido, final String correo) {
            this(credencial, tipoIdentificacionId, numeroIdentificacion, primerNombre, segundoNombre, primerApellido,
                    segundoApellido, correo, null);
        }
    }

    default VentanaAbierta abrirVentana(UUID grupoId, String token, String pin, UUID usuarioEjecutor) {
        return abrirVentana(grupoId, token, pin, usuarioEjecutor, null);
    }

    /** CULM-001 B2NM (S3 minimo): {@code programaId} se guarda como programaPorDefecto de la ventana. */
    VentanaAbierta abrirVentana(UUID grupoId, String token, String pin, UUID usuarioEjecutor, UUID programaId);

    void cerrarVentana(UUID grupoId, UUID usuarioEjecutor);

    VentanaRotada rotarManual(UUID grupoId, String token, String pin, UUID usuarioEjecutor);

    /** Rotacion del job: sin ejecutor; rota solo si la ventana ya vencio su periodo de rotacion. */
    VentanaRotada rotarAutomatico(UUID grupoId, String token, String pin);

    List<VentanaPorRotar> consultarVentanasPorRotar();

    CursoMatricula validar(CredencialMatricula credencial);

    MatriculaRealizada registrarPublico(RegistroPublico datos);

    default MatriculaRealizada matricularAutenticado(final CredencialMatricula credencial, final UUID usuarioEjecutor) {
        return matricularAutenticado(credencial, usuarioEjecutor, null);
    }

    /**
     * DEC-078: matricula autenticada con el programa que eligio el estudiante ({@code programaId}, uno de los
     * {@link CursoMatricula#programasElegibles()}); {@code null} equivale a {@link #matricularAutenticado(CredencialMatricula, UUID)}.
     */
    MatriculaRealizada matricularAutenticado(CredencialMatricula credencial, UUID usuarioEjecutor, UUID programaId);
}
