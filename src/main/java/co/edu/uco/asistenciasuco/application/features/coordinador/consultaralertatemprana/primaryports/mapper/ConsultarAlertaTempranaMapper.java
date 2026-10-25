package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.ConsultarAlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertaTempranaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertasPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.ConsultarAlertaTempranaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAlertasProjection;
import java.util.List;

public final class ConsultarAlertaTempranaMapper {

    private ConsultarAlertaTempranaMapper() {
    }

    public static ConsultarAlertaTempranaDomain toDomain(final ConsultarAlertaTempranaDTO dto) {
        return new ConsultarAlertaTempranaDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico(), dto.semaforo(), dto.nombre(), dto.pagina(), dto.tamano());
    }

    public static AlertasPaginaDTO toDto(final AlertasPaginaDomain d) {
        final List<AlertaTempranaDTO> alertas = d.alertas().stream()
                .map(a -> new AlertaTempranaDTO(a.idEstudiante(), a.nombreCompleto(), a.numeroIdentificacion(), a.correo(), a.totalMatriculasActivas(), a.porcentajeInasistencia(), a.faltasSinJustaCausa(), a.codigoPermanencia(), a.semaforo()))
                .toList();
        return new AlertasPaginaDTO(alertas, d.totalRegistros());
    }
}
