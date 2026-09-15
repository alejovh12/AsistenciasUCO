package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.GestionarAsignaturaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;
import java.util.UUID;

public final class GestionarAsignaturaUseCaseImpl implements GestionarAsignaturaUseCase {

    private final AsignaturaCommandPort commandPort;

    public GestionarAsignaturaUseCaseImpl(final AsignaturaCommandPort commandPort) {
        this.commandPort = Objects.requireNonNull(commandPort, "AsignaturaCommandPort es obligatorio.");
    }

    @Override
    public void crear(final AsignaturaDomain domain) {
        validar(domain);
        commandPort.crearAsignatura(UUID.randomUUID(), domain.codigo(), domain.nombre(), domain.creditos(), domain.idPlanEstudio(),
                domain.semestreNumero(), domain.nombreArea(), domain.nombreComponente());
    }

    @Override
    public void actualizar(final AsignaturaDomain domain) {
        validar(domain);
        commandPort.actualizarAsignatura(domain.idAsignatura(), domain.codigo(), domain.nombre(), domain.creditos(), domain.idPlanEstudio(),
                domain.semestreNumero(), domain.nombreArea(), domain.nombreComponente());
    }

    @Override
    public void toggleEstado(final UUID asignaturaId) {
        if (ObjectHelper.isNull(asignaturaId)) {
            throw new CrosscuttingException("La asignatura es obligatoria.");
        }
        commandPort.toggleEstadoAsignatura(asignaturaId);
    }

    private static void validar(final AsignaturaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para guardar asignatura es obligatorio.");
        }
    }
}
