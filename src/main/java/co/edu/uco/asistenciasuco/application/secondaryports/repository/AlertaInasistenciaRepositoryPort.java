package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoEvaluacion;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.PaginaAlertas;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoResolutores;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 C.1 (HU026C): motor de alertas de inasistencia, grupos pendientes del job, alertas del estudiante y
 * actualizacion de resolutores. La DB decide todo (umbrales por horas, ambito, escalamiento); el backend solo avisa.
 */
public interface AlertaInasistenciaRepositoryPort {

    /** {@code usp_evaluar_alertas_inasistencia_grupo} (comando de sistema). */
    ResultadoEvaluacion evaluar(UUID grupoId, UUID estudianteGrupoId);

    /** {@code usp_consultar_grupos_pendientes_evaluacion_alertas}; con {@code periodoId} todos los grupos del periodo. */
    List<UUID> consultarGruposPendientes(Integer lote, UUID periodoId);

    /** {@code usp_consultar_alertas_estudiante}. */
    PaginaAlertas consultarAlertasEstudiante(UUID usuarioId, int pagina, int tamano, boolean soloNoLeidas, boolean soloVigentes, UUID grupoId);

    /** {@code usp_actualizar_resolutores_revisiones_cancelacion} (job). */
    ResultadoResolutores actualizarResolutores(Integer lote);








}
