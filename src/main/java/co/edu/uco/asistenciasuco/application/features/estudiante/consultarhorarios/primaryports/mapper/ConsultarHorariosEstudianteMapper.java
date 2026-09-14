package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;

public final class ConsultarHorariosEstudianteMapper {

    private ConsultarHorariosEstudianteMapper() {
    }

    public static HorarioEstudianteDTO toDTO(final HorarioEstudianteDomain domain) {
        return new HorarioEstudianteDTO(domain.id(), domain.idEstudiante(), domain.idGrupo(), domain.codigoMateria(),
                domain.nombreMateria(), domain.grupo(), domain.dia(), domain.horaInicio(), domain.horaFin(),
                domain.aula(), domain.docente());
    }
}
