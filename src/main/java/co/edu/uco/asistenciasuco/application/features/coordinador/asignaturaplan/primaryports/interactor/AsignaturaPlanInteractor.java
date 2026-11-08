package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.AsignaturaPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.*;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.AsignaturaPlanUseCase;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAsignaturasPlanProjection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Adaptador del puerto primario: traduce DTO a parametros del caso de uso y proyecciones a DTO. */
public final class AsignaturaPlanInteractor implements AsignaturaPlanInputPort {

    private final AsignaturaPlanUseCase useCase;

    public AsignaturaPlanInteractor(final AsignaturaPlanUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "AsignaturaPlanUseCase es obligatorio.");
    }

    @Override
    public void asociar(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final AsociarAsignaturaPlanDTO dto) {
        if (dto == null) {
            throw new CrosscuttingException("La solicitud es obligatoria.");
        }
        useCase.asociar(idUsuarioEjecutor, idPlanEstudio, dto.idAsignatura(), dto.semestreNumero(), dto.codigoCaracter());
    }

    @Override
    public void desvincular(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final UUID idAsignatura) {
        useCase.desvincular(idUsuarioEjecutor, idPlanEstudio, idAsignatura);
    }

    @Override
    public PaginaAsignaturasAsociablesDTO consultarAsociables(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final String busqueda, final int pagina, final int tamano) {
        final var p = useCase.consultarAsociables(idUsuarioEjecutor, idPlanEstudio, busqueda, pagina, tamano);
        return new PaginaAsignaturasAsociablesDTO(p.elementos().stream().map(a -> new AsignaturaAsociableDTO(a.idAsignatura(), a.codigo(), a.nombre(), a.credito(),
                a.nombreArea(), a.nombreComponente(), a.planes())).toList(), p.total());
    }

    @Override
    public List<CaracterAsignaturaDTO> consultarCaracteres() {
        return useCase.consultarCaracteres().stream().map(c -> new CaracterAsignaturaDTO(c.codigo(), c.nombre(), c.orden())).toList();
    }

    @Override
    public List<ProgramaResumenDTO> consultarProgramasGrupo(final UUID idUsuarioEjecutor, final UUID idGrupo) {
        return useCase.consultarProgramasGrupo(idUsuarioEjecutor, idGrupo).stream()
                .map(p -> new ProgramaResumenDTO(p.idPrograma(), p.nombrePrograma(), p.idPlanEstudio(), p.inpPlanEstudio())).toList();
    }

    @Override
    public PaginaAsignaturasPlanDTO consultarMalla(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final Integer numeroSemestre,
                                                   final String codigoCaracter, final String busqueda, final int pagina, final int tamano) {
        return aDto(useCase.consultarMalla(idUsuarioEjecutor, idPlanEstudio, numeroSemestre, codigoCaracter, busqueda, pagina, tamano));
    }

    @Override
    public PaginaAsignaturasPlanDTO consultarAsignaturasCoordinador(final UUID idUsuarioEjecutor, final UUID idPrograma, final String busqueda,
                                                                    final Boolean estaActiva, final int pagina, final int tamano) {
        return aDto(useCase.consultarAsignaturasCoordinador(idUsuarioEjecutor, idPrograma, busqueda, estaActiva, pagina, tamano));
    }

    @Override
    public PaginaPlanesEstudioDTO consultarPlanes(final UUID idUsuarioEjecutor, final UUID idPrograma, final Boolean estaActivo, final int pagina, final int tamano) {
        final var p = useCase.consultarPlanes(idUsuarioEjecutor, idPrograma, estaActivo, pagina, tamano);
        return new PaginaPlanesEstudioDTO(p.elementos().stream().map(x -> new co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.dto.PlanEstudioDTO(x.id(), x.idPrograma(), x.nombrePrograma(), x.inp(),
                x.estaActivoPlanEstudio(), x.estaActivoTextoPlanEstudio(), x.justificacionEstado(), x.totalSemestres(), x.totalAsignaturas(), x.totalCreditos())).toList(), p.total());
    }

    @Override
    public PaginaGruposFacultadDTO consultarGruposFacultad(final UUID idUsuarioEjecutor, final UUID idPeriodoAcademico, final UUID idPrograma,
                                                           final String busqueda, final int pagina, final int tamano) {
        final var p = useCase.consultarGruposFacultad(idUsuarioEjecutor, idPeriodoAcademico, idPrograma, busqueda, pagina, tamano);
        return new PaginaGruposFacultadDTO(p.elementos().stream().map(g -> new GrupoFacultadDTO(g.idGrupo(), g.codigo(), g.nombre(), g.idPeriodoAcademico(),
                g.idAsignatura(), g.nombreAsignatura(), g.idDocente(), g.capacidadMaximaPermitida(), g.estudiantesActivos(), g.cuposDisponibles(),
                g.grupoEstaHablitado(), g.programas())).toList(), p.total());
    }

    private static PaginaAsignaturasPlanDTO aDto(final PaginaAsignaturasPlanProjection p) {
        return new PaginaAsignaturasPlanDTO(p.elementos().stream().map(a -> new AsignaturaPlanDTO(a.idAsignatura(), a.codigo(), a.nombre(), a.credito(), a.idArea(),
                a.nombreArea(), a.idComponente(), a.nombreComponente(), a.idPlanEstudio(), a.inpPlanEstudio(), a.idPrograma(), a.nombrePrograma(),
                a.idSemestrePlanEstudio(), a.numeroSemestre(), a.codigoSemestre(), a.codigoCaracter(), a.nombreCaracter(), a.estaActivoVinculo(),
                a.estaActivaAsignatura(), a.compartida())).toList(), p.total());
    }
}
