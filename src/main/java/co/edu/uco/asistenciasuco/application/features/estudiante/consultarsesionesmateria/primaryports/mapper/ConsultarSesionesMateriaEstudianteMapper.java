package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.TomaSesionEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.TomaSesionEstudianteDomain;

public final class ConsultarSesionesMateriaEstudianteMapper {

    private ConsultarSesionesMateriaEstudianteMapper() {
    }

    public static SesionMateriaEstudianteDTO toDTO(final SesionMateriaEstudianteDomain domain) {
        return new SesionMateriaEstudianteDTO(domain.id(), domain.nombre(), domain.numero(), domain.codigo(),
                domain.numeroSemana(), domain.idGrupo(), domain.codigoGrupo(), domain.nombreGrupo(),
                domain.fechaHoraInicio(), domain.fechaHoraFin(), domain.fechaHoraInicioUtc(), domain.fechaHoraFinUtc(),
                domain.estadoTemporal().name(), domain.estadoAsistencia().name(),
                domain.estadoSolicitudRevision(), domain.reclamable(),
                EstadoSesion.de(domain.cancelacion() != null).name(), domain.cancelacion(),
                domain.fechaLimiteJustificacionUtc(), domain.plazoJustificacionVigente(),
                domain.minutosSesion(), domain.minutosAsistidos(), domain.minutosFalta(), domain.minutosExcusa(),
                domain.minutosSinRegistro(), domain.tomas().stream().map(ConsultarSesionesMateriaEstudianteMapper::toDTO).toList());
    }

    static TomaSesionEstudianteDTO toDTO(final TomaSesionEstudianteDomain toma) {
        return new TomaSesionEstudianteDTO(toma.idToma(), toma.numero(), toma.fechaPuntoControlUtc(), toma.estado(),
                toma.reclamable(), toma.idSolicitudRevision(), toma.estadoSolicitudRevision(), toma.resultadoAprobacion());
    }
}
