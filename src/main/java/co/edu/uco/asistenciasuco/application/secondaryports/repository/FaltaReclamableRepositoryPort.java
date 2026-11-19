package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ATA D10 (HU026B): faltas por reclamar del estudiante, sobre {@code usp_consultar_faltas_reclamables_estudiante}
 * ({@code uv_falta_reclamable_estudiante}). La DB valida el perfil ({@code SEC_001}) y la pagina ({@code PAG_001}).
 */
public interface FaltaReclamableRepositoryPort {

    PaginaFaltasReclamables consultarEstudiante(UUID usuarioId, int pagina, int tamano);

    /** Fila del SP; las horas son DATETIME2 en UTC ya convertidos a instantes. */
    record FilaFaltaReclamable(UUID idGrupo, String nombreGrupo, UUID idAsignatura, String nombreAsignatura, UUID idSesion, int numeroSesion,
                               Instant fechaHoraInicioUtc, Instant fechaHoraFinUtc, UUID idToma, int numeroToma,
                               Instant fechaPuntoControlUtc, Instant fechaLimiteReclamoUtc) {
    }

    /** Pagina de filas y total de filas del filtro ({@code totalRegistros}). */
    record PaginaFaltasReclamables(List<FilaFaltaReclamable> filas, long total) {
        public PaginaFaltasReclamables {
            filas = filas == null ? List.of() : List.copyOf(filas);
        }
    }
}
