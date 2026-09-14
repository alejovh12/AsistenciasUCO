package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.ConsultarEstudiantesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;

import java.util.List;

public interface ConsultarEstudiantesGrupoInputPort {

    List<EstudianteGrupoDTO> execute(ConsultarEstudiantesGrupoDTO dto);
}
