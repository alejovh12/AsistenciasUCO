package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ConsultarFichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.FichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.GrupoHistorialDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ResumenCargaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.ConsultarFichaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.FichaDocenteDomain;

public final class ConsultarFichaDocenteMapper {

    private ConsultarFichaDocenteMapper() {
    }

    public static ConsultarFichaDocenteDomain toDomain(final ConsultarFichaDocenteDTO dto) {
        return dto == null ? null
                : new ConsultarFichaDocenteDomain(dto.actorUsuarioId(), dto.idDocente(), dto.idPeriodoAcademico(), dto.pagina(), dto.tamano());
    }

    public static FichaDocenteDTO toDto(final FichaDocenteDomain d) {
        return new FichaDocenteDTO(
                DocenteProgramaMapper.toDTO(d.docente()),
                new ResumenCargaDocenteDTO(d.resumen().totalGrupos(), d.resumen().totalPeriodos(), d.resumen().gruposVigentes(),
                        d.resumen().estudiantesActivosVigentes()),
                d.historial().stream()
                        .map(g -> new GrupoHistorialDocenteDTO(g.idGrupo(), g.codigoGrupo(), g.nombreGrupo(), g.idAsignatura(),
                                g.nombreAsignatura(), g.idPeriodoAcademico(), g.codigoPeriodoAcademico(), g.nombrePeriodoAcademico(),
                                g.fechaInicioPeriodo(), g.fechaFinPeriodo(), g.estadoPeriodo(), g.capacidadMaxima(),
                                g.estudiantesActivos(), g.estudiantesFinalizados(), g.estudiantesCanceladosVoluntad(),
                                g.estudiantesCanceladosInasistencia()))
                        .toList(),
                d.totalHistorial());
    }
}
