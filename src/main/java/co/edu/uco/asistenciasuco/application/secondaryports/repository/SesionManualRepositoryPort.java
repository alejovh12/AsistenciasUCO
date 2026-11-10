package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): sesiones creadas manualmente por el docente. La DB decide que una sesion es manual
 * ({@code uv_sesion_manual}), calcula los destinatarios del aviso, valida el ambito del coordinador ({@code SEC_002}) y
 * aplica las reglas de cancelacion ({@code SES_011}, {@code SES_012}, {@code SES_016}, {@code SES_018}, {@code SES_019},
 * {@code SES_021}).
 */
public interface SesionManualRepositoryPort {

    /** {@code usp_consultar_destinatarios_sesion_manual}: estudiantes activos y coordinadores activos de los programas del grupo. */
    List<DestinatarioSesionManual> consultarDestinatarios(UUID sesionId, UUID docenteUsuarioId);

    /** {@code usp_cancelar_sesion_coordinador}. */
    SesionCancelada cancelarPorCoordinador(UUID sesionId, String codigoMotivo, String observacion, UUID coordinadorUsuarioId);

    /** {@code usp_consultar_motivos_cancelacion_sesion}: catalogo de motivos activos, en su orden. */
    List<MotivoCancelacion> consultarMotivosCancelacion(UUID coordinadorUsuarioId);

    /** Destinatario del aviso: {@code tipo} es {@code ESTUDIANTE} o {@code COORDINADOR}. */
    record DestinatarioSesionManual(String tipo, UUID idUsuario, String nombreCompleto, String correo, String nombreAsignatura,
                                    String nombreGrupo, String nombreDocente, LocalDateTime fechaHoraInicio,
                                    LocalDateTime fechaHoraFin) {

        public static final String TIPO_ESTUDIANTE = "ESTUDIANTE";
        public static final String TIPO_COORDINADOR = "COORDINADOR";
    }

    record MotivoCancelacion(String codigo, String nombre) {
    }
}
