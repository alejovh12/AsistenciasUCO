package co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.dto.DocenteCandidatoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.domain.DocenteCandidatoDomain;

public final class ConsultarCandidatosDocenteMapper {

    private ConsultarCandidatosDocenteMapper() {
    }

    public static DocenteCandidatoDTO toDTO(final DocenteCandidatoDomain domain) {
        return new DocenteCandidatoDTO(domain.idDocente(), domain.idUsuario(), domain.codigoTipoIdentificacion(),
                domain.numeroIdentificacion(), domain.nombreCompleto());
    }
}
