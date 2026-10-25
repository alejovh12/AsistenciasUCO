package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ConsultarComparativaFacultadesDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ConsultarComparativaFacultadesDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ComparativaFacultadDomain;
import java.util.List;

public final class ConsultarComparativaFacultadesMapper {

    private ConsultarComparativaFacultadesMapper() {
    }

    public static ConsultarComparativaFacultadesDomain toDomain(final ConsultarComparativaFacultadesDTO dto) {
        return new ConsultarComparativaFacultadesDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico());
    }

    public static List<ComparativaFacultadDTO> toDto(final List<ComparativaFacultadDomain> filas) {
        return filas.stream().map(d -> new ComparativaFacultadDTO(d.idFacultad(), d.nombreFacultad(), d.estaActivaFacultad(), d.totalProgramas(), d.totalEstudiantes(), d.estudiantesRiesgo(), d.estudiantesPerdida(), d.tasaRetencion(), d.sesionesEjecutadas(), d.faltasSinJustaCausa(), d.porcentajeAusentismo())).toList();
    }
}
