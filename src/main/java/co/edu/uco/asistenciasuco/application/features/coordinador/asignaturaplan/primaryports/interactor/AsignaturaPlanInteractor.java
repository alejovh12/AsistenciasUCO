package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.AsignaturaPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.*;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.AsignaturaPlanUseCase;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
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
}
