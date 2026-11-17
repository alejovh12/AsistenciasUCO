package co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.ParametrosPaginacion;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.result.MatrizAsistenciaGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.usecase.ConsultarMatrizAsistenciaGrupoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatrizAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenSesionesGrupoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;

/**
 * CIERRE-W3M (punto 1): la titularidad se valida aqui (403) y de nuevo en el SP. La pagina de estudiantes con sus celdas sale de
 * {@code usp_consultar_matriz_asistencia_grupo_docente_paginado}; el encabezado de sesiones (con su estado temporal) sale del
 * resumen de sesiones del grupo, de modo que ambas lecturas comparten las reglas de la base.
 */
public final class ConsultarMatrizAsistenciaGrupoUseCaseImpl implements ConsultarMatrizAsistenciaGrupoUseCase {

    private final InstitutionalScopePort scope;
    private final MatrizAsistenciaQueryPort matriz;
    private final ResumenSesionesGrupoQueryPort sesiones;

    public ConsultarMatrizAsistenciaGrupoUseCaseImpl(final InstitutionalScopePort scope, final MatrizAsistenciaQueryPort matriz,
                                                    final ResumenSesionesGrupoQueryPort sesiones) {
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.matriz = Objects.requireNonNull(matriz, "MatrizAsistenciaQueryPort es obligatorio.");
        this.sesiones = Objects.requireNonNull(sesiones, "ResumenSesionesGrupoQueryPort es obligatorio.");
    }

    @Override
    public MatrizAsistenciaGrupoDTO execute(final Consulta consulta) {
        if (consulta == null || consulta.grupoId() == null || consulta.usuarioEjecutor() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_008);
        }
        if (!scope.canDocenteAccessGrupo(consulta.usuarioEjecutor(), consulta.grupoId())) {
            throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_002);
        }
        final String busqueda = consulta.busqueda() == null || consulta.busqueda().isBlank() ? null : consulta.busqueda().trim();
        final PaginaResultado<MatrizAsistenciaQueryPort.FilaMatriz> pagina = matriz.consultarMatrizGrupoDocente(
                consulta.grupoId(), ParametrosPaginacion.pagina(consulta.pagina()), ParametrosPaginacion.tamano(consulta.tamano()),
                busqueda, consulta.usuarioEjecutor());
        final List<MatrizAsistenciaGrupoDTO.Sesion> encabezado = sesiones
                .consultarResumenSesionesGrupoDocente(consulta.grupoId(), consulta.usuarioEjecutor()).sesiones().stream()
                .map(s -> new MatrizAsistenciaGrupoDTO.Sesion(s.idSesion(), s.numero(), s.nombre(), s.estadoSesion(),
                        s.fechaHoraInicioUtc(), s.fechaHoraFinUtc(), s.horarioConfirmado()))
                .toList();
        return new MatrizAsistenciaGrupoDTO(encabezado,
                pagina.elementos().stream().map(f -> new MatrizAsistenciaGrupoDTO.Fila(f.idEstudianteGrupo(), f.idEstudiante(),
                        f.nombreCompleto(), f.documento(), f.codigoEstadoEstudiante(),
                        f.celdas().stream().map(c -> new MatrizAsistenciaGrupoDTO.Celda(c.idSesion(), c.codigoEstado(),
                                c.minutosAsistidos(), c.minutosSesion())).toList())).toList(),
                pagina.numeroPagina(), pagina.tamanoPagina(), pagina.totalElementos());
    }
}
