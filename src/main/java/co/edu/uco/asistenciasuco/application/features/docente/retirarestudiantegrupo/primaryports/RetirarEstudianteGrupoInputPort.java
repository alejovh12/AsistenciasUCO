package co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.dto.RetirarEstudianteGrupoDTO;

public interface RetirarEstudianteGrupoInputPort {

    void execute(RetirarEstudianteGrupoDTO dto);
}
