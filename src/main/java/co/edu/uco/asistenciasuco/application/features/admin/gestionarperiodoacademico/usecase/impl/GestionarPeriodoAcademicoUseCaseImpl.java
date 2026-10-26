package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.GestionarPeriodoAcademicoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.domain.PeriodoAcademicoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoCommandPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * HU096/HU097/HU098. Valida la forma de la entrada; la DB revalida el perfil ADMINISTRADOR (el periodo es unico
 * por institucion; DECANO y COORDINADOR solo lo consultan), resuelve la institucion del ejecutor y aplica la
 * unicidad del nombre y la regla de eliminar solo periodos sin grupos.
 */
public final class GestionarPeriodoAcademicoUseCaseImpl implements GestionarPeriodoAcademicoUseCase {

    private final PeriodoAcademicoCommandPort commandPort;

    public GestionarPeriodoAcademicoUseCaseImpl(final PeriodoAcademicoCommandPort commandPort) {
        this.commandPort = Objects.requireNonNull(commandPort, "PeriodoAcademicoCommandPort es obligatorio.");
    }

    @Override
    public void crear(final PeriodoAcademicoDomain domain) {
        validar(domain);
        commandPort.crearPeriodoAcademico(UUID.randomUUID(), domain.nombre().trim(), domain.fechaInicio(),
                domain.fechaFin(), domain.usuario());
    }

    @Override
    public void actualizar(final PeriodoAcademicoDomain domain) {
        validar(domain);
        if (ObjectHelper.isNull(domain.idPeriodo())) {
            throw new ValidationException("El periodo academico a modificar es obligatorio.");
        }
        commandPort.actualizarPeriodoAcademico(domain.idPeriodo(), domain.nombre().trim(), domain.fechaInicio(),
                domain.fechaFin(), domain.usuario());
    }

    @Override
    public void eliminar(final UUID idPeriodo, final UUID usuario) {
        if (ObjectHelper.isNull(idPeriodo)) {
            throw new ValidationException("El periodo academico a eliminar es obligatorio.");
        }
        commandPort.eliminarPeriodoAcademico(idPeriodo, usuario);
    }

    private static void validar(final PeriodoAcademicoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para guardar periodo academico es obligatorio.");
        }
        if (ObjectHelper.isNull(domain.nombre()) || domain.nombre().isBlank()) {
            throw new ValidationException("El nombre del periodo academico es obligatorio.");
        }
        if (ObjectHelper.isNull(domain.fechaInicio()) || ObjectHelper.isNull(domain.fechaFin())
                || !domain.fechaInicio().isBefore(domain.fechaFin())) {
            throw new ValidationException("La fecha de inicio del periodo debe ser anterior a la fecha de fin.");
        }
    }
}
