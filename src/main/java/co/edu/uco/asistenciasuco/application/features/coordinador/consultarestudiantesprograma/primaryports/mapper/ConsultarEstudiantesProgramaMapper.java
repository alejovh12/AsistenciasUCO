package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.EstudianteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.EstudianteProgramaDomain;

public final class ConsultarEstudiantesProgramaMapper {

    private ConsultarEstudiantesProgramaMapper() {
    }

    public static EstudianteProgramaDTO toDTO(final EstudianteProgramaDomain domain) {
        return new EstudianteProgramaDTO(
                domain.id(),
                domain.idUsuario(),
                domain.numeroIdentificacion(),
                domain.nombreCompleto(),
                domain.correo(),
                domain.idPrograma(),
                domain.nombrePrograma()
        );
    }
}
