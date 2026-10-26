package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.ConsultarAsistenciaProgramaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.AsistenciaSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.GrupoProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.SesionGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * HU064/HU060/HU061. Exige un coordinador con programa (403 si no lo tiene) y valida paginacion y filtros; el
 * aislamiento por programa lo garantiza el provider DB, que filtra por el usuario ejecutor.
 */
public final class ConsultarAsistenciaProgramaUseCaseImpl implements ConsultarAsistenciaProgramaUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 10;
    static final int TAMANO_MAXIMO = 100;

    private final AsistenciaProgramaQueryPort queryPort;
    private final InstitutionalScopePort scopePort;

    public ConsultarAsistenciaProgramaUseCaseImpl(final AsistenciaProgramaQueryPort queryPort,
                                                  final InstitutionalScopePort scopePort) {
        this.queryPort = Objects.requireNonNull(queryPort, "AsistenciaProgramaQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public Pagina<GrupoProgramaProjection> consultarGrupos(final UUID usuario, final UUID idPeriodoAcademico,
                                                            final String busqueda, final Integer pagina,
                                                            final Integer tamano) {
        exigirCoordinador(usuario);
        final String filtro = ObjectHelper.isNull(busqueda) || busqueda.isBlank() ? null : busqueda.trim();
        return queryPort.consultarGrupos(usuario, idPeriodoAcademico, filtro, pagina(pagina), tamano(tamano));
    }

    @Override
    public Pagina<SesionGrupoProjection> consultarSesionesGrupo(final UUID usuario, final UUID idGrupo,
                                                                 final LocalDate desde, final LocalDate hasta,
                                                                 final Integer pagina, final Integer tamano) {
        exigirCoordinador(usuario);
        exigirId(idGrupo, "El grupo es obligatorio.");
        if (!ObjectHelper.isNull(desde) && !ObjectHelper.isNull(hasta) && desde.isAfter(hasta)) {
            throw new ValidationException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        return queryPort.consultarSesionesGrupo(usuario, idGrupo, desde, hasta, pagina(pagina), tamano(tamano));
    }

    @Override
    public Pagina<AsistenciaSesionProjection> consultarAsistenciasSesion(final UUID usuario, final UUID idSesion,
                                                                          final Boolean asistio, final Integer pagina,
                                                                          final Integer tamano) {
        exigirCoordinador(usuario);
        exigirId(idSesion, "La sesion es obligatoria.");
        return queryPort.consultarAsistenciasSesion(usuario, idSesion, asistio, pagina(pagina), tamano(tamano));
    }

    private void exigirCoordinador(final UUID usuario) {
        if (ObjectHelper.isNull(usuario) || scopePort.findProgramaIdByCoordinadorUsuario(usuario).isEmpty()) {
            throw new ForbiddenException("No fue posible resolver el programa del coordinador autenticado.");
        }
    }

    private static void exigirId(final UUID id, final String mensaje) {
        if (ObjectHelper.isNull(id)) {
            throw new ValidationException(mensaje);
        }
    }

    private static int pagina(final Integer pagina) {
        if (ObjectHelper.isNull(pagina)) {
            return PAGINA_POR_DEFECTO;
        }
        if (pagina < 1) {
            throw new ValidationException("La pagina debe ser mayor o igual a 1.");
        }
        return pagina;
    }

    private static int tamano(final Integer tamano) {
        if (ObjectHelper.isNull(tamano)) {
            return TAMANO_POR_DEFECTO;
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ValidationException("El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        return tamano;
    }
}
