package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.ConsultarFacultadDecanoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstructuraFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstudianteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.MatriculaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.SesionFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU104/HU105/HU109/HU119/HU145. Exige un decano con facultad (403 si no la tiene) y valida paginacion y filtros;
 * el aislamiento por facultad lo garantiza el provider DB, que filtra por el decano ejecutor.
 */
public final class ConsultarFacultadDecanoUseCaseImpl implements ConsultarFacultadDecanoUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 10;
    static final int TAMANO_MAXIMO = 100;
    static final int BUSQUEDA_MAXIMA = 100;

    private final FacultadDecanoQueryPort queryPort;
    private final InstitutionalScopePort scopePort;

    public ConsultarFacultadDecanoUseCaseImpl(final FacultadDecanoQueryPort queryPort,
                                              final InstitutionalScopePort scopePort) {
        this.queryPort = Objects.requireNonNull(queryPort, "FacultadDecanoQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public Pagina<EstudianteFacultadProjection> consultarEstudiantes(final UUID usuario, final String busqueda,
                                                                     final UUID idPrograma, final Boolean estaActivo,
                                                                     final Integer pagina, final Integer tamano) {
        exigirDecano(usuario);
        final String filtro = ObjectHelper.isNull(busqueda) || busqueda.isBlank() ? null : busqueda.trim();
        if (!ObjectHelper.isNull(filtro) && filtro.length() > BUSQUEDA_MAXIMA) {
            throw new ValidationException("La busqueda excede la longitud permitida.");
        }
        return queryPort.consultarEstudiantes(usuario, filtro, idPrograma, estaActivo, pagina(pagina), tamano(tamano));
    }

    @Override
    public List<MatriculaEstudianteProjection> consultarDetalleEstudiante(final UUID usuario, final UUID idEstudiante) {
        exigirDecano(usuario);
        if (ObjectHelper.isNull(idEstudiante)) {
            throw new ValidationException("El estudiante es obligatorio.");
        }
        final List<MatriculaEstudianteProjection> matriculas = queryPort.consultarDetalleEstudiante(usuario, idEstudiante);
        if (matriculas.isEmpty()) {
            throw new ResourceNotFoundException("El estudiante no existe en la facultad del decano.");
        }
        return matriculas;
    }

    @Override
    public List<EstructuraFacultadProjection> consultarEstructura(final UUID usuario) {
        exigirDecano(usuario);
        return queryPort.consultarEstructura(usuario);
    }

    @Override
    public Pagina<SesionFacultadProjection> consultarSesiones(final UUID usuario, final LocalDate desde,
                                                              final LocalDate hasta, final UUID idPrograma,
                                                              final Integer pagina, final Integer tamano) {
        exigirDecano(usuario);
        if (!ObjectHelper.isNull(desde) && !ObjectHelper.isNull(hasta) && desde.isAfter(hasta)) {
            throw new ValidationException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        return queryPort.consultarSesiones(usuario, desde, hasta, idPrograma, pagina(pagina), tamano(tamano));
    }

    private void exigirDecano(final UUID usuario) {
        if (ObjectHelper.isNull(usuario) || scopePort.findFacultadIdByDecanoUsuario(usuario).isEmpty()) {
            throw new ForbiddenException("No fue posible resolver la facultad del decano autenticado.");
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
