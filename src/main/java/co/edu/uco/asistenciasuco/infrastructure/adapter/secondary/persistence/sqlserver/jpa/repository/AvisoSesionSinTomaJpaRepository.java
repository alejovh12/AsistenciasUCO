package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort;
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
 * NOTOMA-001: aviso a la coordinacion de la sesion sin toma via procedimientos almacenados de sistema (JPA, {@code EXEC} nativo). Las dos
 * consultas leen cada columna por su NOMBRE ({@link Tuple}); el registro de la marca es un comando con la tupla canonica. Sin usuario: lo
 * ejecuta el job. Los correos de los coordinadores son de uso interno del {@code NotificacionPort}: no se registran en logs.
 */
@Repository
public class AvisoSesionSinTomaJpaRepository implements AvisoSesionSinTomaRepositoryPort {

    private static final String P_CORRELACION = "idCorrelacion";
    private static final String P_SESION = "idSesion";

    static final String SQL_PENDIENTES = "EXEC dbo.usp_consultar_sesiones_pendientes_aviso_sin_toma @idCorrelacion = :idCorrelacion";
    static final String SQL_DESTINATARIOS = "EXEC dbo.usp_consultar_destinatarios_aviso_sin_toma @idSesion = :idSesion, @idCorrelacion = :idCorrelacion";
    static final String SQL_REGISTRAR = """
            EXEC dbo.usp_registrar_aviso_sesion_sin_toma
                 @idSesion = :idSesion, @totalCoordinadoresAvisados = :totalCoordinadoresAvisados, @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AvisoSesionSinTomaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del aviso de sesion sin toma es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public List<SesionSinTomaPendiente> consultarSesionesPendientes() {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarSesionesPendientesAvisoSinToma", "No fue posible consultar las sesiones pendientes de aviso de sesion sin toma.", () -> {
            final List<Tuple> filas = tuplas(SQL_PENDIENTES, Map.of(P_CORRELACION, correlacion));
            final List<SesionSinTomaPendiente> pendientes = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                pendientes.add(new SesionSinTomaPendiente(NativeRowValues.uuid(f.get(P_SESION)), NativeRowValues.uuid(f.get("idGrupo")),
                        NativeRowValues.instanteUtc(f.get("fechaHoraFinUtc"))));
            }
            return pendientes;
        });
    }

    @Override
    public List<DestinatarioAvisoSinToma> consultarDestinatarios(final UUID sesionId) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarDestinatariosAvisoSinToma", "No fue posible consultar los coordinadores del aviso de sesion sin toma.", () -> {
            final List<Tuple> filas = tuplas(SQL_DESTINATARIOS, Map.of(P_SESION, sesionId, P_CORRELACION, correlacion));
            final List<DestinatarioAvisoSinToma> destinatarios = new ArrayList<>(filas.size());
            for (final Tuple f : filas) {
                destinatarios.add(new DestinatarioAvisoSinToma(NativeRowValues.uuid(f.get(P_SESION)), NativeRowValues.uuid(f.get("idGrupo")),
                        NativeRowValues.uuid(f.get("idAsignatura")), NativeRowValues.texto(f.get("nombreAsignatura")),
                        NativeRowValues.texto(f.get("nombreGrupo")), NativeRowValues.texto(f.get("nombreDocente")),
                        NativeRowValues.instanteUtc(f.get("fechaHoraInicioUtc")), NativeRowValues.instanteUtc(f.get("fechaHoraFinUtc")),
                        NativeRowValues.uuid(f.get("idUsuarioCoordinador")), NativeRowValues.texto(f.get("nombreCoordinador")),
                        NativeRowValues.texto(f.get("correoCoordinador"))));
            }
            return destinatarios;
        });
    }

    @Override
    public void registrarAviso(final UUID sesionId, final int totalCoordinadoresAvisados) {
        final UUID correlacion = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(P_SESION, sesionId);
        parametros.put("totalCoordinadoresAvisados", totalCoordinadoresAvisados);
        parametros.put(P_CORRELACION, correlacion);
        procedureExecutor.execute("registrarAvisoSesionSinToma", SQL_REGISTRAR, parametros, correlacion);
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> tuplas(final String sql, final Map<String, Object> parametros) {
        final var consulta = entityManager.createNativeQuery(sql, Tuple.class);
        parametros.forEach(consulta::setParameter);
        return consulta.getResultList();
    }
}
