package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ConsultarFichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ResumenAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.ConsultarFichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.FichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.ResumenAsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FichaEstudianteProjection;
import java.util.List;

public final class ConsultarFichaEstudianteMapper {

    private ConsultarFichaEstudianteMapper() {
    }

    public static ConsultarFichaEstudianteDomain toDomain(final ConsultarFichaEstudianteDTO dto) {
        return new ConsultarFichaEstudianteDomain(dto.actorUsuarioId(), dto.idEstudiante(), dto.idPeriodoAcademico());
    }

    public static FichaEstudianteDTO toDto(final FichaEstudianteDomain d) {
        final List<ResumenAsignaturaDTO> resumen = d.resumenPorAsignatura().stream()
                .map(r -> new ResumenAsignaturaDTO(r.idGrupo(), r.codigoGrupo(), r.idAsignatura(), r.nombreAsignatura(), r.codigoEstadoEstudiante(), r.nombreEstadoEstudiante(), r.sesionesEjecutadas(), r.asistencias(), r.faltasSinJustaCausa(), r.excusas(), r.porcentajeInasistencia(), r.codigoPermanencia()))
                .toList();
        return new FichaEstudianteDTO(d.idEstudiante(), d.tipoIdentificacion(), d.numeroIdentificacion(), d.primerNombre(), d.segundoNombre(), d.primerApellido(), d.segundoApellido(), d.nombreCompleto(), d.correo(), d.estaActivoUsuario(), d.idPrograma(), d.nombrePrograma(), resumen);
    }
}
