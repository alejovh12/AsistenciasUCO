package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.ConsultarBitacoraEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraSesionDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.ConsultarBitacoraEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaBitacoraProjection;
import java.util.List;

public final class ConsultarBitacoraEstudianteMapper {

    private ConsultarBitacoraEstudianteMapper() {
    }

    public static ConsultarBitacoraEstudianteDomain toDomain(final ConsultarBitacoraEstudianteDTO dto) {
        return new ConsultarBitacoraEstudianteDomain(dto.actorUsuarioId(), dto.idEstudiante(), dto.idPeriodoAcademico(), dto.pagina(), dto.tamano());
    }

    public static BitacoraPaginaDTO toDto(final BitacoraPaginaDomain d) {
        final List<BitacoraSesionDTO> sesiones = d.sesiones().stream()
                .map(s -> new BitacoraSesionDTO(s.idSesion(), s.numeroSesion(), s.nombreSesion(), s.fechaHoraInicio(), s.fechaHoraFin(), s.idGrupo(), s.codigoGrupo(), s.idAsignatura(), s.nombreAsignatura(), s.asistio(), s.codigoRazonCausa(), s.nombreRazonCausa()))
                .toList();
        return new BitacoraPaginaDTO(sesiones, d.totalRegistros());
    }
}
