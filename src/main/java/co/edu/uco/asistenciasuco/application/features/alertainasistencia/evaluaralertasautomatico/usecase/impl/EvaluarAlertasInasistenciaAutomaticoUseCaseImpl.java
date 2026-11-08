package co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.primaryports.EvaluarAlertasInasistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.usecase.EvaluarAlertasInasistenciaAutomaticoUseCase;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AlertaInasistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.NovedadResolutores;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoResolutores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 C.1 (contrato 3.4): cada pasada (1) evalua con aviso los grupos pendientes y (2) recalcula los resolutores de las revisiones
 * que esperan al coordinador o al decano, avisando las reasignaciones. Un grupo o una revision que falla no detiene a los demas.
 */
public final class EvaluarAlertasInasistenciaAutomaticoUseCaseImpl implements EvaluarAlertasInasistenciaAutomaticoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluarAlertasInasistenciaAutomaticoUseCaseImpl.class);

    private final AlertaInasistenciaRepositoryPort repositorio;
    private final EvaluadorAlertasInasistencia evaluador;
    private final AvisoRevisionCancelacionService avisos;

    public EvaluarAlertasInasistenciaAutomaticoUseCaseImpl(
            final AlertaInasistenciaRepositoryPort repositorio,
            final EvaluadorAlertasInasistencia evaluador,
            final AvisoRevisionCancelacionService avisos
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "AlertaInasistenciaRepositoryPort es obligatorio.");
        this.evaluador = Objects.requireNonNull(evaluador, "EvaluadorAlertasInasistencia es obligatorio.");
        this.avisos = Objects.requireNonNull(avisos, "AvisoRevisionCancelacionService es obligatorio.");
    }

    @Override
    public Integer execute() {
        int evaluados = 0;
        try {
            final List<UUID> grupos = repositorio.consultarGruposPendientes(null, null);
            for (final UUID grupo : grupos) {
                evaluador.evaluarYAvisar(grupo, null);
                evaluados++;
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible listar los grupos pendientes de evaluacion: {}", exception.getClass().getSimpleName());
        }
        try {
            final ResultadoResolutores resultado = repositorio.actualizarResolutores(null);
            for (final NovedadResolutores novedad : resultado.novedades()) {
                avisos.avisarReasignacion(novedad);
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible actualizar los resolutores de las revisiones: {}", exception.getClass().getSimpleName());
        }
        return evaluados;
    }
}
