package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): sesiones manuales via {@code usp_consultar_destinatarios_sesion_manual},
 * {@code usp_cancelar_sesion_coordinador} y {@code usp_consultar_motivos_cancelacion_sesion} (JPA, {@code EXEC} nativo). El SP de
 * cancelacion toma {@code Sesion WITH (UPDLOCK, HOLDLOCK)}, igual que {@code usp_cancelar_sesion}.
 */
@Repository
public class SesionManualJpaRepository implements SesionManualRepositoryPort {

    static final String OP_CANCELAR_COORDINADOR = "cancelarSesionCoordinador";
    static final String SQL_CANCELAR_COORDINADOR = """
            EXEC dbo.usp_cancelar_sesion_coordinador
                 @idSesion = :idSesion, @codigoMotivo = :codigoMotivo, @observacion = :observacion,
                 @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_DESTINATARIOS = """
            EXEC dbo.usp_consultar_destinatarios_sesion_manual
                 @idSesion = :idSesion, @idCorrelacion = :idCorrelacion, @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_MOTIVOS = """
            EXEC dbo.usp_consultar_motivos_cancelacion_sesion
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public SesionManualJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de sesiones manuales es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public SesionCancelada cancelarPorCoordinador(final UUID sesionId, final String codigoMotivo, final String observacion,
                                                  final UUID coordinadorUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idSesion", sesionId);
        parametros.put("codigoMotivo", codigoMotivo);
        parametros.put("observacion", observacion);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", coordinadorUsuarioId);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CANCELAR_COORDINADOR, SQL_CANCELAR_COORDINADOR, parametros, correlationId, 4);
        final List<Object> c = fila.extras();
        return new SesionCancelada(NativeRowValues.uuid(c.get(0)), NativeRowValues.uuid(c.get(1)),
                SesionCancelacionJpaRepository.fechaHora(c.get(2)), c.get(3) == null ? 0 : ((Number) c.get(3)).intValue());
    }

    @Override
    public List<DestinatarioSesionManual> consultarDestinatarios(final UUID sesionId, final UUID docenteUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarDestinatariosSesionManual",
                "No fue posible consultar los destinatarios del aviso de la sesion manual.",
                () -> {
                    final List<?> filas;
                    try {
                        filas = entityManager.createNativeQuery(SQL_DESTINATARIOS)
                                .setParameter("idSesion", sesionId)
                                .setParameter("idCorrelacion", correlationId)
                                .setParameter("idUsuarioEjecutor", docenteUsuarioId)
                                .getResultList();
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception, "sesion");
                    }
                    final List<DestinatarioSesionManual> destinatarios = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] c = (Object[]) fila;
                        destinatarios.add(new DestinatarioSesionManual(
                                texto(c[0]), NativeRowValues.uuid(c[1]), texto(c[2]), texto(c[3]), texto(c[4]), texto(c[5]),
                                texto(c[6]), SesionCancelacionJpaRepository.fechaHora(c[7]), SesionCancelacionJpaRepository.fechaHora(c[8])));
                    }
                    return destinatarios;
                });
    }

    @Override
    public List<MotivoCancelacion> consultarMotivosCancelacion(final UUID coordinadorUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarMotivosCancelacionSesion",
                "No fue posible consultar los motivos de cancelacion de sesion.",
                () -> {
                    final List<?> filas;
                    try {
                        filas = entityManager.createNativeQuery(SQL_MOTIVOS)
                                .setParameter("idUsuarioEjecutor", coordinadorUsuarioId)
                                .setParameter("idCorrelacion", correlationId)
                                .getResultList();
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception, "sesion");
                    }
                    final List<MotivoCancelacion> motivos = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] c = (Object[]) fila;
                        motivos.add(new MotivoCancelacion(texto(c[0]), texto(c[1])));
                    }
                    return motivos;
                });
    }

    private static String texto(final Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
