package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ConsultarComparativaProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ConsultarComparativaProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ComparativaProgramaDomain;
import java.util.List;

public final class ConsultarComparativaProgramasMapper {

    private ConsultarComparativaProgramasMapper() {
    }

    public static ConsultarComparativaProgramasDomain toDomain(final ConsultarComparativaProgramasDTO dto) {
        return new ConsultarComparativaProgramasDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico());
    }

    public static List<ComparativaProgramaDTO> toDto(final List<ComparativaProgramaDomain> filas) {
        return filas.stream().map(d -> new ComparativaProgramaDTO(d.idPrograma(), d.nombrePrograma(), d.estaActivoPrograma(), d.totalEstudiantes(), d.estudiantesRiesgo(), d.estudiantesPerdida(), d.tasaRetencion(), d.sesionesEjecutadas(), d.faltasSinJustaCausa(), d.porcentajeAusentismo())).toList();
    }
}
