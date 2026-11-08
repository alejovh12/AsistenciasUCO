package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistroAsistenciaSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.ConsultarAsistenciasPorGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciasSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.SolicitarRevisionAsistenciaRequest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AsistenciaHttpMapper {

    private AsistenciaHttpMapper() {
    }

    public static ConsultarAsistenciasPorGrupoDTO toApplicationDTO(
            final ConsultarAsistenciasPorGrupoRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(request, "El request HTTP para consultar asistencias por grupo es obligatorio.");
        return new ConsultarAsistenciasPorGrupoDTO(request.getGrupo(), request.getSesion(), usuarioEjecutor);
    }

    /**
     * CULM-001 D.2: {@code soporteArchivoIds} (0..N) o, por compatibilidad, {@code soporteArchivoId} como lista de uno. El validador
     * del controlador ya rechazo el caso de ambos a la vez.
     */
    private static List<UUID> soportesDe(final SolicitarRevisionAsistenciaRequest request) {
        if (request.getSoporteArchivoIds() != null) {
            return request.getSoporteArchivoIds();
        }
        return request.getSoporteArchivoId() == null ? List.of() : List.of(request.getSoporteArchivoId());
    }

    public static SolicitarRevisionAsistenciaDTO toApplicationDTO(
            final SolicitarRevisionAsistenciaRequest request,
            final UUID usuarioId
    ) {
        Objects.requireNonNull(request, "El request HTTP para solicitar revision de asistencia es obligatorio.");
        return new SolicitarRevisionAsistenciaDTO(
                request.getSesionId(),
                request.getCategoria(),
                request.getJustificacion(),
                soportesDe(request),
                usuarioId
        );
    }

    public static RegistrarAsistenciasSesionDTO toApplicationDTO(
            final RegistrarAsistenciasSesionRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(request, "El request HTTP para registrar asistencias de sesion es obligatorio.");
        final List<RegistroAsistenciaSesionDTO> registros = request.getRegistros() == null
                ? List.of()
                : request.getRegistros().stream()
                        .map(registro -> new RegistroAsistenciaSesionDTO(
                                registro == null ? null : registro.getEstudianteId(),
                                registro == null ? null : registro.getEstado()
                        ))
                        .toList();
        return new RegistrarAsistenciasSesionDTO(request.getSesionId(), registros, usuarioEjecutor);
    }
}
