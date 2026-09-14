package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain.HorarioDocenteDomain;

public final class ConsultarHorariosDocenteMapper {

    private ConsultarHorariosDocenteMapper() {
    }

    public static HorarioDocenteDTO toDTO(final HorarioDocenteDomain domain) {
        return new HorarioDocenteDTO(domain.id(), domain.idDocente(), domain.idGrupo(), domain.codigoMateria(),
                domain.nombreMateria(), domain.seccion(), domain.dia(), domain.horaInicio(), domain.horaFin(),
                domain.aula(), domain.totalEstudiantes());
    }
}
