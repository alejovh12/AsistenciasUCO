package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.TomaSesionEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MinutosSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;

import java.util.List;

public final class ConsultarSesionesMateriaEstudianteRepositoryMapper {

    private ConsultarSesionesMateriaEstudianteRepositoryMapper() {
    }

    public static SesionMateriaEstudianteDomain toDomain(final SesionMateriaEstudianteProjection projection,
                                                         final SesionProcedenciaEntity procedencia,
                                                         final EstadoAsistenciaSesion estadoAsistencia,
                                                         final String estadoSolicitudRevision,
                                                         final boolean reclamable) {
        return toDomain(projection, procedencia, estadoAsistencia, estadoSolicitudRevision, reclamable, null, List.of());
    }

    /** PC (puntos de control): agrega los minutos de la sesion ({@code minutos} nulo si no hay cabecera) y sus tomas. */
    public static SesionMateriaEstudianteDomain toDomain(final SesionMateriaEstudianteProjection projection,
                                                         final SesionProcedenciaEntity procedencia,
                                                         final EstadoAsistenciaSesion estadoAsistencia,
                                                         final String estadoSolicitudRevision,
                                                         final boolean reclamable,
                                                         final MinutosSesionEstudianteProjection minutos,
                                                         final List<TomaSesionEstudianteDomain> tomas) {
        return new SesionMateriaEstudianteDomain(projection.id(), projection.nombre(), projection.numero(),
                projection.codigo(), projection.numeroSemana(), projection.idGrupo(), projection.codigoGrupo(),
                projection.nombreGrupo(), projection.fechaHoraInicio(), projection.fechaHoraFin(),
                procedencia == null ? null : procedencia.fechaHoraInicioUtc(),
                procedencia == null ? null : procedencia.fechaHoraFinUtc(),
                procedencia == null ? EstadoHorarioSesion.INDETERMINADA : procedencia.estadoTemporal(),
                estadoAsistencia, estadoSolicitudRevision, reclamable,
                procedencia == null ? null : procedencia.cancelacion(),
                projection.fechaLimiteJustificacionUtc(), projection.plazoJustificacionVigente(),
                minutos == null ? null : minutos.minutosSesion(), minutos == null ? null : minutos.minutosAsistidos(),
                minutos == null ? null : minutos.minutosFalta(), minutos == null ? null : minutos.minutosExcusa(),
                minutos == null ? null : minutos.minutosSinRegistro(), tomas);
    }
}
