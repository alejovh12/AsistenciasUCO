package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.AsignaturaPlanUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.*;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaPlanPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Orquestacion de las asignaturas por plan: valida entradas obligatorias y delega en el SP (el ambito vive en la BD). */
public final class AsignaturaPlanUseCaseImpl implements AsignaturaPlanUseCase {

    static final int TAMANO_MAXIMO = 100;
    private final AsignaturaPlanPort port;

    public AsignaturaPlanUseCaseImpl(final AsignaturaPlanPort port) {
        this.port = Objects.requireNonNull(port, "AsignaturaPlanPort es obligatorio.");
    }

    @Override
    public void asociar(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final UUID idAsignatura, final Integer semestreNumero, final String codigoCaracter) {
        exigir(idUsuarioEjecutor, "El usuario ejecutor es obligatorio.");
        exigir(idPlanEstudio, "El plan de estudios es obligatorio.");
        exigir(idAsignatura, "La asignatura es obligatoria.");
        exigir(semestreNumero, "El semestre es obligatorio.");
        port.asociar(idUsuarioEjecutor, idPlanEstudio, idAsignatura, semestreNumero, codigoCaracter);
    }

    @Override
    public void desvincular(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final UUID idAsignatura) {
        exigir(idUsuarioEjecutor, "El usuario ejecutor es obligatorio.");
        exigir(idPlanEstudio, "El plan de estudios es obligatorio.");
        exigir(idAsignatura, "La asignatura es obligatoria.");
        port.desvincular(idUsuarioEjecutor, idPlanEstudio, idAsignatura);
    }

    @Override
    public PaginaAsignaturasAsociablesProjection consultarAsociables(final UUID idUsuarioEjecutor, final UUID idPlanEstudio, final String busqueda, final int pagina, final int tamano) {
        exigir(idUsuarioEjecutor, "El usuario ejecutor es obligatorio.");
        exigir(idPlanEstudio, "El plan de estudios es obligatorio.");
        return port.consultarAsociables(idUsuarioEjecutor, idPlanEstudio, busqueda, Math.max(pagina, 1), Math.min(Math.max(tamano, 1), TAMANO_MAXIMO));
    }

    @Override
    public List<CaracterAsignaturaProjection> consultarCaracteres() {
        return port.consultarCaracteres();
    }

    @Override
    public List<ProgramaGrupoProjection> consultarProgramasGrupo(final UUID idUsuarioEjecutor, final UUID idGrupo) {
        exigir(idUsuarioEjecutor, "El usuario ejecutor es obligatorio.");
        exigir(idGrupo, "El grupo es obligatorio.");
        return port.consultarProgramasGrupo(idUsuarioEjecutor, idGrupo);
    }

    private static void exigir(final Object valor, final String mensaje) {
        if (valor == null) {
            throw new CrosscuttingException(mensaje);
        }
    }
}
