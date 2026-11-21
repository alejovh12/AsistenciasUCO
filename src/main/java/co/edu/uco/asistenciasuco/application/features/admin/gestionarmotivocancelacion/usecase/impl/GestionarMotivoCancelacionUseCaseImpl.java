package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.GestionarMotivoCancelacionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.domain.MotivoCancelacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MotivoCancelacionAdminPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarMotivoCancelacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Administracion del catalogo de motivos de cancelacion de sesion (migracion 65 de la BD). Valida la forma de la entrada; la DB exige el perfil
 * ADMINISTRADOR (SEC_001), el largo y los caracteres de los textos, la unicidad del codigo y del nombre y que quede al menos
 * un registro activo. Nunca se borra: se activa o desactiva.
 */
public final class GestionarMotivoCancelacionUseCaseImpl implements GestionarMotivoCancelacionUseCase {

    private final MotivoCancelacionAdminPort port;

    public GestionarMotivoCancelacionUseCaseImpl(final MotivoCancelacionAdminPort port) {
        this.port = Objects.requireNonNull(port, "MotivoCancelacionAdminPort es obligatorio.");
    }

    @Override
    public List<MotivoCancelacionAdminProjection> consultar() {
        return port.consultarTodos();
    }

    @Override
    public ResultadoCatalogoAdminProjection crear(final MotivoCancelacionDomain dom) {
        validarEntrada(dom);
        if (ObjectHelper.isNull(dom.codigo())) {
            throw new CrosscuttingException("El codigo es obligatorio.");
        }
        return port.crear(new GuardarMotivoCancelacionCommand(dom.id() == null ? UUID.randomUUID() : dom.id(), dom.codigo().strip(), dom.nombre().strip(), dom.orden(), dom.usuario()));
    }

    @Override
    public ResultadoCatalogoAdminProjection actualizar(final MotivoCancelacionDomain dom) {
        validarEntrada(dom);
        if (ObjectHelper.isNull(dom.id())) {
            throw new CrosscuttingException("El identificador es obligatorio.");
        }
        return port.actualizar(new GuardarMotivoCancelacionCommand(dom.id(), null, dom.nombre().strip(), dom.orden(), dom.usuario()));
    }

    @Override
    public ResultadoCatalogoAdminProjection cambiarEstado(final UUID id, final Boolean estaActivo, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(id) || ObjectHelper.isNull(estaActivo) || ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("El identificador, el estado y el usuario ejecutor son obligatorios.");
        }
        return port.cambiarEstado(id, estaActivo, usuarioEjecutor);
    }

    private static void validarEntrada(final MotivoCancelacionDomain dom) {
        if (ObjectHelper.isNull(dom) || ObjectHelper.isNull(dom.nombre()) || ObjectHelper.isNull(dom.usuario())) {
            throw new CrosscuttingException("El nombre y el usuario ejecutor son obligatorios.");
        }
    }
}
