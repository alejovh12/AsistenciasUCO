package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CIERRE-W3M (puntos 2 y 3): resumen de las sesiones del grupo del docente titular con su estado temporal y sus conteos, y los
 * indicadores del grupo ({@code usp_consultar_resumen_sesiones_grupo_docente}). La hora actual y los umbrales los resuelve la base.
 */
public interface ResumenSesionesGrupoQueryPort {

    /** Sesion del grupo; {@code estadoSesion} es PROGRAMADA, EN_CURSO, EJECUTADA o CANCELADA. */
    record SesionResumen(UUID idSesion, int numero, String nombre, Instant fechaHoraInicioUtc, Instant fechaHoraFinUtc,
                         boolean horarioConfirmado, String estadoSesion, String estadoSolicitud, boolean pendienteAprobacion,
                         int totalEstudiantes, int presentes, int parciales, int justificadas, int ausentes, int sinRegistro) {
    }

    /** Resumen del grupo; {@code promedioAsistencia} es {@code null} si aun no hay horas dictadas. */
    record ResumenGrupo(int sesionesProgramadas, int sesionesEjecutadas, int sesionesCanceladas, int sesionesEnCurso,
                        UUID idSesionEnCurso, UUID idSesionProxima, int estudiantesActivos, int estudiantesEnRiesgo,
                        BigDecimal promedioAsistencia, BigDecimal umbralAdvertencia, BigDecimal umbralPerdida,
                        List<SesionResumen> sesiones) {

        public ResumenGrupo {
            sesiones = sesiones == null ? List.of() : List.copyOf(sesiones);
        }
    }

    ResumenGrupo consultarResumenSesionesGrupoDocente(UUID grupoId, UUID usuarioEjecutor);
}
