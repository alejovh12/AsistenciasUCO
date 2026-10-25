package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AlertaTempranaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.BitacoraSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FichaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAlertasProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaBitacoraProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenAsignaturaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpAlertaTempranaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpBitacoraSesionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpFichaEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpKpisCoordinadorEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpResumenAsignaturaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** KPIs, alerta temprana, bitacora y ficha del coordinador: ejecuta los SP de consulta y mapea fielmente sus columnas. */
@Repository
public class CoordinadorIndicadoresJpaRepository implements CoordinadorIndicadoresQueryPort {

    static final String SQL_KPIS = """
            EXEC dbo.usp_consultar_kpis_coordinador
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico
            """;

    static final String SQL_ALERTAS = """
            EXEC dbo.usp_consultar_alerta_temprana_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @semaforo = :semaforo,
                 @filtroTexto = :filtroTexto
            """;

    static final String SQL_BITACORA = """
            EXEC dbo.usp_consultar_bitacora_estudiante_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idEstudiante = :idEstudiante,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @idPeriodoAcademico = :idPeriodoAcademico
            """;

    static final String SQL_FICHA = """
            EXEC dbo.usp_consultar_ficha_estudiante_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idEstudiante = :idEstudiante
            """;

    static final String SQL_RESUMEN = """
            EXEC dbo.usp_consultar_resumen_asistencia_estudiante_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idEstudiante = :idEstudiante,
                 @idPeriodoAcademico = :idPeriodoAcademico
            """;

    private final EntityManager entityManager;

    public CoordinadorIndicadoresJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de indicadores es obligatorio.");
    }

    @Override
    public KpisCoordinadorProjection consultarKpis(final UUID actorUsuarioId, final UUID idPeriodoAcademico) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarKpisCoordinador", "No fue posible consultar los indicadores del programa desde base de datos.",
                () -> {
                    final List<SpKpisCoordinadorEntity> filas = ejecutar(SQL_KPIS, SpKpisCoordinadorEntity.class, correlationId, actorUsuarioId,
                            Map.of(), idPeriodoAcademico);
                    if (filas.isEmpty()) {
                        throw new IllegalStateException("usp_consultar_kpis_coordinador no devolvio fila.");
                    }
                    final SpKpisCoordinadorEntity e = filas.getFirst();
                    return new KpisCoordinadorProjection(e.idPrograma(), e.nombrePrograma(), e.idPeriodoAcademico(), e.nombrePeriodoAcademico(), cero(e.totalEstudiantes()), cero(e.estudiantesRegulares()), cero(e.estudiantesRiesgo()), cero(e.estudiantesPerdida()), e.tasaPermanencia(), cero(e.sesionesProgramadas()), cero(e.sesionesVencidas()), cero(e.sesionesEjecutadas()), cero(e.sesionesVencidasSinRegistro()), cero(e.sesionesSinProcedenciaTemporal()), e.porcentajeCumplimiento(), e.porcentajeProgresoCalendario(), e.umbralAdvertencia(), e.umbralPerdida());
                });
    }

    @Override
    public PaginaAlertasProjection consultarAlertas(final UUID actorUsuarioId, final UUID idPeriodoAcademico, final String semaforo, final String nombre,
                                                    final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarAlertaTemprana", "No fue posible consultar las alertas tempranas desde base de datos.",
                () -> {
                    final Map<String, Object> extra = new LinkedHashMap<>();
                    extra.put("numeroPagina", pagina);
                    extra.put("tamanoPagina", tamano);
                    extra.put("semaforo", semaforo);
                    extra.put("filtroTexto", nombre);
                    final List<SpAlertaTempranaEntity> filas = ejecutar(SQL_ALERTAS, SpAlertaTempranaEntity.class, correlationId, actorUsuarioId, extra, idPeriodoAcademico);
                    final int total = filas.isEmpty() ? 0 : cero(filas.getFirst().totalRegistros());
                    return new PaginaAlertasProjection(filas.stream().map(e -> new AlertaTempranaProjection(e.idEstudiante(), e.nombreCompleto(), e.numeroIdentificacion(), e.correo(), cero(e.totalMatriculasActivas()), e.porcentajeInasistencia(), cero(e.faltasSinJustaCausa()), e.codigoPermanencia(), e.semaforo())).toList(), total);
                });
    }

    @Override
    public PaginaBitacoraProjection consultarBitacora(final UUID actorUsuarioId, final UUID idEstudiante, final UUID idPeriodoAcademico,
                                                      final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarBitacoraEstudiante", "No fue posible consultar la bitacora del estudiante desde base de datos.",
                () -> {
                    final Map<String, Object> extra = new LinkedHashMap<>();
                    extra.put("idEstudiante", idEstudiante);
                    extra.put("numeroPagina", pagina);
                    extra.put("tamanoPagina", tamano);
                    final List<SpBitacoraSesionEntity> filas = ejecutar(SQL_BITACORA, SpBitacoraSesionEntity.class, correlationId, actorUsuarioId, extra, idPeriodoAcademico);
                    final int total = filas.isEmpty() ? 0 : cero(filas.getFirst().totalRegistros());
                    return new PaginaBitacoraProjection(filas.stream().map(e -> new BitacoraSesionProjection(e.idSesion(), cero(e.numeroSesion()), e.nombreSesion(), utc(e.fechaHoraInicio()), utc(e.fechaHoraFin()), e.idGrupo(), cero(e.codigoGrupo()), e.idAsignatura(), e.nombreAsignatura(), e.asistio(), e.codigoRazonCausa(), e.nombreRazonCausa())).toList(), total);
                });
    }

    @Override
    public FichaEstudianteProjection consultarFicha(final UUID actorUsuarioId, final UUID idEstudiante) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarFichaEstudiante", "No fue posible consultar la ficha del estudiante desde base de datos.",
                () -> {
                    final List<SpFichaEstudianteEntity> filas = ejecutar(SQL_FICHA, SpFichaEstudianteEntity.class, correlationId, actorUsuarioId,
                            Map.of("idEstudiante", idEstudiante), null, false);
                    if (filas.isEmpty()) {
                        throw new IllegalStateException("usp_consultar_ficha_estudiante_programa no devolvio fila.");
                    }
                    final SpFichaEstudianteEntity e = filas.getFirst();
                    return new FichaEstudianteProjection(e.idEstudiante(), e.tipoIdentificacion(), e.numeroIdentificacion(), e.primerNombre(), e.segundoNombre(), e.primerApellido(), e.segundoApellido(), e.nombreCompleto(), e.correo(), Boolean.TRUE.equals(e.estaActivoUsuario()), e.idPrograma(), e.nombrePrograma());
                });
    }

    @Override
    public List<ResumenAsignaturaProjection> consultarResumenAsistencia(final UUID actorUsuarioId, final UUID idEstudiante, final UUID idPeriodoAcademico) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarResumenAsistenciaEstudiante", "No fue posible consultar el resumen de asistencia desde base de datos.",
                () -> ejecutar(SQL_RESUMEN, SpResumenAsignaturaEntity.class, correlationId, actorUsuarioId, Map.of("idEstudiante", idEstudiante), idPeriodoAcademico).stream()
                        .map(e -> new ResumenAsignaturaProjection(e.idGrupo(), cero(e.codigoGrupo()), e.idAsignatura(), e.nombreAsignatura(), e.codigoEstadoEstudiante(), e.nombreEstadoEstudiante(), cero(e.sesionesEjecutadas()), cero(e.asistencias()), cero(e.faltasSinJustaCausa()), cero(e.excusas()), e.porcentajeInasistencia(), e.codigoPermanencia())).toList());
    }

    private <T> List<T> ejecutar(final String sql, final Class<T> tipo, final UUID correlationId, final UUID actor, final Map<String, Object> parametros,
                                 final UUID idPeriodoAcademico) {
        return ejecutar(sql, tipo, correlationId, actor, parametros, idPeriodoAcademico, true);
    }

    private <T> List<T> ejecutar(final String sql, final Class<T> tipo, final UUID correlationId, final UUID actor, final Map<String, Object> parametros,
                                 final UUID idPeriodoAcademico, final boolean conPeriodo) {
        try {
            final Query query = entityManager.createNativeQuery(sql, tipo)
                    .setParameter("idUsuarioEjecutor", actor)
                    .setParameter("idCorrelacion", correlationId);
            for (final Map.Entry<String, Object> parametro : parametros.entrySet()) {
                query.setParameter(parametro.getKey(), parametro.getValue());
            }
            if (conPeriodo) {
                query.setParameter("idPeriodoAcademico", idPeriodoAcademico);
            }
            return query.getResultList().stream().map(tipo::cast).toList();
        } catch (final PersistenceException exception) {
            throw DbCodeTranslator.traducir(exception);
        }
    }

    private static int cero(final Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static Instant utc(final LocalDateTime valor) {
        return SoporteArchivoJpaRepository.toUtcInstant(valor);
    }
}
