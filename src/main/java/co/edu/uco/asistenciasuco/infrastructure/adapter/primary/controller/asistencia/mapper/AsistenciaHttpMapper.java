package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.ConsultarAsistenciasPorGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.SolicitarRevisionAsistenciaRequest;

import java.util.Objects;

public final class AsistenciaHttpMapper {

    private AsistenciaHttpMapper() {
    }

    public static RegistrarAsistenciaDTO toApplicationDTO(final RegistrarAsistenciaRequest request) {
        Objects.requireNonNull(request, "El request HTTP para registrar asistencia es obligatorio.");
        return new RegistrarAsistenciaDTO(
                request.getEstudiante(),
                request.getGrupo(),
                request.getSesion(),
                request.getPresente(),
                request.getObservacion()
        );
    }

    public static ConsultarAsistenciasPorGrupoDTO toApplicationDTO(final ConsultarAsistenciasPorGrupoRequest request) {
        Objects.requireNonNull(request, "El request HTTP para consultar asistencias por grupo es obligatorio.");
        return new ConsultarAsistenciasPorGrupoDTO(request.getGrupo(), request.getSesion());
    }

    public static SolicitarRevisionAsistenciaDTO toApplicationDTO(final SolicitarRevisionAsistenciaRequest request) {
        Objects.requireNonNull(request, "El request HTTP para solicitar revision de asistencia es obligatorio.");
        return new SolicitarRevisionAsistenciaDTO(request.getAsistencia(), request.getMotivo());
    }
}
