package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.ConsultarTendenciaAusentismoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.ConsultarTendenciaAusentismoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.PuntoTendenciaDomain;
import java.util.List;

public final class ConsultarTendenciaAusentismoMapper {

    private ConsultarTendenciaAusentismoMapper() {
    }

    public static ConsultarTendenciaAusentismoDomain toDomain(final ConsultarTendenciaAusentismoDTO dto) {
        return new ConsultarTendenciaAusentismoDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico(), dto.granularidad());
    }

    public static List<PuntoTendenciaDTO> toDto(final List<PuntoTendenciaDomain> filas) {
        return filas.stream().map(d -> new PuntoTendenciaDTO(d.inicioIntervalo(), d.finIntervalo(), d.sesionesEjecutadas(), d.faltasSinJustaCausa(), d.porcentajeAusentismo())).toList();
    }
}
