package co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.PaginaAlertas;

import java.util.UUID;

/** CULM-001 C.1 (HU026C/HU013): alertas del estudiante autenticado (todas sus matriculas), paginadas y filtrables. */
public interface ConsultarAlertasEstudianteInputPort
        extends InteractorWithReturn<ConsultarAlertasEstudianteInputPort.Comando, PaginaAlertas> {

    record Comando(UUID usuarioId, Integer pagina, Integer tamano, boolean soloNoLeidas, boolean soloVigentes, UUID grupoId) {
    }
}
