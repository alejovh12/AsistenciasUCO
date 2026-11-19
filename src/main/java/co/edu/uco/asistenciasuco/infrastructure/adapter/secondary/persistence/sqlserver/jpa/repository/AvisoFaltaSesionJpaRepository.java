package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoFaltaSesionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * ATA D1 (HU175): aviso de falta por sesion via procedimientos almacenados de sistema (JPA, {@code EXEC} nativo). Las dos consultas leen cada
 * columna por su NOMBRE ({@link Tuple}); el registro de la marca es un comando con la tupla canonica. Sin usuario: lo ejecuta el job.
 * Los correos de los estudiantes son de uso interno del {@code NotificacionPort}: no se registran en logs.
 */
@Repository
public class AvisoFaltaSesionJpaRepository implements AvisoFaltaSesionRepositoryPort {

    private static final String P_CORRELACION = "idCorrelacion";
    private static final String P_SESION = "idSesion";

    static final String SQL_PENDIENTES = "EXEC dbo.usp_consultar_sesiones_pendientes_aviso_falta @idCorrelacion = :idCorrelacion";
    static final String SQL_FALTAS = "EXEC dbo.usp_consultar_faltas_sesion_aviso @idSesion = :idSesion, @idCorrelacion = :idCorrelacion";
    static final String SQL_REGISTRAR = """
            EXEC dbo.usp_registrar_aviso_falta_sesion
                 @idSesion = :idSesion, @totalEstudiantesAvisados = :totalEstudiantesAvisados, @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AvisoFaltaSesionJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del aviso de falta es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public List<SesionPendienteAviso> consultarSesionesPendientes() {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarSesionesPendientesAvisoFalta", "No fue posible consultar las sesiones pendientes de aviso de falta.", () -> {
            final List<Tuple> filas = tuplas(SQL_PENDIENTES, Map.of(P_CORRELACION, correlacion));
            final List<SesionPendienteAviso> pendientes = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                pendientes.add(new SesionPendienteAviso(NativeRowValues.uuid(f.get(P_SESION)), NativeRowValues.uuid(f.get("idGrupo")),
                        NativeRowValues.instanteUtc(f.get("fechaHoraFinUtc"))));
            }
            return pendientes;
        });
    }

    @Override
    public List<FaltaSesionAviso> consultarFaltas(final UUID sesionId) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarFaltasSesionAviso", "No fue posible consultar las faltas de la sesion.", () -> {
            final List<Tuple> filas = tuplas(SQL_FALTAS, Map.of(P_SESION, sesionId, P_CORRELACION, correlacion));
            final List<FaltaSesionAviso> faltas = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                faltas.add(new FaltaSesionAviso(NativeRowValues.uuid(f.get(P_SESION)), NativeRowValues.uuid(f.get("idGrupo")),
                        NativeRowValues.uuid(f.get("idAsignatura")), NativeRowValues.texto(f.get("nombreAsignatura")),
                        NativeRowValues.texto(f.get("nombreGrupo")), NativeRowValues.instanteUtc(f.get("fechaHoraInicioUtc")),
                        NativeRowValues.instanteUtc(f.get("fechaHoraFinUtc")), NativeRowValues.uuid(f.get("idUsuarioEstudiante")),
                        NativeRowValues.texto(f.get("nombreEstudiante")), NativeRowValues.texto(f.get("correoEstudiante")),
                        NativeRowValues.entero(f.get("numeroToma")), NativeRowValues.instanteUtc(f.get("fechaPuntoControlUtc")),
                        NativeRowValues.entero(f.get("totalTomasSesion")), NativeRowValues.entero(f.get("totalTomasEnFalta")),
                        NativeRowValues.entero(f.get("minutosFalta")), NativeRowValues.instanteUtc(f.get("fechaLimiteReclamoUtc"))));
            }
            return faltas;
        });
    }

    @Override
    public void registrarAviso(final UUID sesionId, final int totalEstudiantesAvisados) {
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_SESION, sesionId);
        parametros.put("totalEstudiantesAvisados", totalEstudiantesAvisados);
        parametros.put(P_CORRELACION, correlacion);
        procedureExecutor.execute("registrarAvisoFaltaSesion", SQL_REGISTRAR, parametros, correlacion);
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> tuplas(final String sql, final Map<String, Object> parametros) {
        final var consulta = entityManager.createNativeQuery(sql, Tuple.class);
        parametros.forEach(consulta::setParameter);
        return consulta.getResultList();
    }
}
