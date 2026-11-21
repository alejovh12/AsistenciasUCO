package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.GestionarCategoriaJustificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.domain.CategoriaJustificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CategoriaJustificacionAdminPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarCategoriaJustificacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Administracion del catalogo de categorias de justificacion (migracion 65 de la BD). Valida la forma de la entrada; la DB exige el perfil
 * ADMINISTRADOR (SEC_001), el largo y los caracteres de los textos, la unicidad del codigo y del nombre y que quede al menos
 * un registro activo. Nunca se borra: se activa o desactiva.
 */
public final class GestionarCategoriaJustificacionUseCaseImpl implements GestionarCategoriaJustificacionUseCase {

    private final CategoriaJustificacionAdminPort port;

    public GestionarCategoriaJustificacionUseCaseImpl(final CategoriaJustificacionAdminPort port) {
        this.port = Objects.requireNonNull(port, "CategoriaJustificacionAdminPort es obligatorio.");
    }

    @Override
    public List<CategoriaJustificacionAdminProjection> consultar() {
        return port.consultarTodos();
    }

    @Override
    public ResultadoCatalogoAdminProjection crear(final CategoriaJustificacionDomain dom) {
        validarEntrada(dom);
        if (ObjectHelper.isNull(dom.codigo())) {
            throw new CrosscuttingException("El codigo es obligatorio.");
        }
        return port.crear(new GuardarCategoriaJustificacionCommand(dom.id() == null ? UUID.randomUUID() : dom.id(), dom.codigo().strip(), dom.nombre().strip(), texto(dom.descripcion()), dom.requiereSoporte(), dom.usuario()));
    }

    @Override
    public ResultadoCatalogoAdminProjection actualizar(final CategoriaJustificacionDomain dom) {
        validarEntrada(dom);
        if (ObjectHelper.isNull(dom.id())) {
            throw new CrosscuttingException("El identificador es obligatorio.");
        }
        return port.actualizar(new GuardarCategoriaJustificacionCommand(dom.id(), null, dom.nombre().strip(), texto(dom.descripcion()), dom.requiereSoporte(), dom.usuario()));
    }

    @Override
    public ResultadoCatalogoAdminProjection cambiarEstado(final UUID id, final Boolean estaActiva, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(id) || ObjectHelper.isNull(estaActiva) || ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("El identificador, el estado y el usuario ejecutor son obligatorios.");
        }
        return port.cambiarEstado(id, estaActiva, usuarioEjecutor);
    }

    private static void validarEntrada(final CategoriaJustificacionDomain dom) {
        if (ObjectHelper.isNull(dom) || ObjectHelper.isNull(dom.nombre()) || ObjectHelper.isNull(dom.usuario())) {
            throw new CrosscuttingException("El nombre y el usuario ejecutor son obligatorios.");
        }
        if (ObjectHelper.isNull(dom.requiereSoporte())) {
            throw new CrosscuttingException("El campo requiereSoporte es obligatorio.");
        }
    }

    private static String texto(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
