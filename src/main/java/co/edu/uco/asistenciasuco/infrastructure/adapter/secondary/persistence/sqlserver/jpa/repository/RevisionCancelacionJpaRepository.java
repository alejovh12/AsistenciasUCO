package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.*;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CancelacionRowSupport.bit;
import static co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CancelacionRowSupport.decimal;
import static co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CancelacionRowSupport.entero;
import static co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CancelacionRowSupport.instante;
import static co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CancelacionRowSupport.texto;
import static co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CancelacionRowSupport.uuid;

/**
 * CULM-001 C.1 (HU026C): decision del docente ({@code usp_decidir_revision_cancelacion}), resolucion del coordinador
 * ({@code usp_resolver_revision_cancelacion}; el decano recibe RCN_016, DEC-065), bandejas (coordinador, decano y docente) y destinatarios.
 * El ambito y las transiciones los fuerza la DB (SEC_002, COO_003, RCN_001..RCN_016).
 */
@Repository
public class RevisionCancelacionJpaRepository implements RevisionCancelacionRepositoryPort {

    static final String OP_DECIDIR = "decidirRevisionCancelacion";
    static final String OP_RESOLVER = "resolverRevisionCancelacion";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public RevisionCancelacionJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de revisiones es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public ResultadoDecision decidir(final UUID revisionId, final String decision, final String motivo, final UUID usuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = comando(revisionId, decision, motivo, usuarioId, correlationId);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_DECIDIR,
                CancelacionRowSupport.exec("usp_decidir_revision_cancelacion", parametros), parametros, correlationId, 12);
        final List<Object> c = fila.extras();
        return new ResultadoDecision(fila.result().getMensajeUsuarioResultado(), uuid(c.get(0)), texto(c.get(1)), texto(c.get(2)), texto(c.get(3)),
                bit(c.get(4)), uuid(c.get(5)), uuid(c.get(6)), uuid(c.get(7)), decimal(c.get(8)), texto(c.get(9)), texto(c.get(10)), bit(c.get(11)));
    }

    @Override
    public ResultadoResolucion resolver(final UUID revisionId, final String decision, final String motivo, final UUID usuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = comando(revisionId, decision, motivo, usuarioId, correlationId);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_RESOLVER,
                CancelacionRowSupport.exec("usp_resolver_revision_cancelacion", parametros), parametros, correlationId, 13);
        final List<Object> c = fila.extras();
        return new ResultadoResolucion(fila.result().getMensajeUsuarioResultado(), uuid(c.get(0)), texto(c.get(1)), texto(c.get(2)), texto(c.get(3)),
                bit(c.get(4)), bit(c.get(5)), uuid(c.get(6)), uuid(c.get(7)), uuid(c.get(8)), uuid(c.get(9)), texto(c.get(10)), decimal(c.get(11)),
                texto(c.get(12)));
    }

    @Override
    public PaginaRevisiones consultarCoordinador(final UUID usuarioId, final int pagina, final int tamano, final boolean soloPendientes,
                                                 final String estado, final UUID programaId, final UUID grupoId) {
        final Map<String, Object> parametros = bandeja(usuarioId, pagina, tamano, soloPendientes, estado, programaId);
        parametros.put("idGrupo", grupoId);
        return consultar("consultarRevisionesCancelacionCoordinador", "usp_consultar_revisiones_cancelacion_coordinador_paginado", parametros);
    }

    @Override
    public PaginaRevisiones consultarDecano(final UUID usuarioId, final int pagina, final int tamano, final boolean soloPendientes,
                                            final String estado, final UUID programaId) {
        return consultar("consultarRevisionesCancelacionDecano", "usp_consultar_revisiones_cancelacion_decano_paginado",
                bandeja(usuarioId, pagina, tamano, soloPendientes, estado, programaId));
    }

    @Override
    public PaginaRevisiones consultarDocente(final UUID usuarioId, final int pagina, final int tamano, final boolean soloPendientes,
                                             final String estado, final UUID grupoId) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idUsuario", usuarioId);
        parametros.put("soloPendientes", soloPendientes ? 1 : 0);
        parametros.put("idGrupo", grupoId);
        parametros.put("estado", estado == null || estado.isBlank() ? null : estado.strip());
        parametros.put("numeroPagina", pagina);
        parametros.put("tamanoPagina", tamano);
        return consultar("consultarRevisionesCancelacionDocente", "usp_consultar_revisiones_cancelacion_docente_paginado", parametros);
    }

    @Override
    public List<DestinatarioRevision> consultarDestinatarios(final UUID usuarioId, final UUID revisionId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarDestinatariosRevisionCancelacion",
                "No fue posible consultar los destinatarios de la revision de cancelacion.", () -> {
                    final Map<String, Object> parametros = new LinkedHashMap<>();
                    parametros.put("idUsuarioEjecutor", usuarioId);
                    parametros.put("idCorrelacion", correlationId);
                    parametros.put("idRevision", revisionId);
                    final List<?> filas = ejecutar("usp_consultar_destinatarios_revision_cancelacion", parametros);
                    final List<DestinatarioRevision> destinatarios = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        final Object[] c = (Object[]) fila;
                        destinatarios.add(new DestinatarioRevision(texto(c[0]), texto(c[1]), uuid(c[2]), texto(c[3]), texto(c[4]), uuid(c[5]), bit(c[6]),
                                uuid(c[7]), entero(c[8]), texto(c[9]), texto(c[10]), texto(c[11]), texto(c[12]), texto(c[13]), decimal(c[14]),
                                decimal(c[15]), texto(c[16]), texto(c[17]), texto(c[18])));
                    }
                    return destinatarios;
                });
    }

    private PaginaRevisiones consultar(final String operacion, final String procedimiento, final Map<String, Object> parametros) {
        final UUID correlationId = CorrelationIdContext.require();
        parametros.put("idCorrelacion", correlationId);
        return JpaQueryExecutor.execute(operacion, "No fue posible consultar las revisiones de cancelacion.", () -> {
            final List<?> filas = ejecutar(procedimiento, parametros);
            final List<RevisionFila> revisiones = new ArrayList<>(filas.size());
            long total = 0;
            for (final Object fila : filas) {
                final Object[] c = (Object[]) fila;
                total = Math.max(total, entero(c[53]));
                revisiones.add(new RevisionFila(uuid(c[0]), texto(c[16]), texto(c[17]), bit(c[19]), bit(c[20]), bit(c[51]), acciones(c[52]),
                        texto(c[33]), instante(c[34]), uuid(c[2]), texto(c[4]), texto(c[14]), uuid(c[5]), entero(c[6]), texto(c[7]), uuid(c[8]),
                        texto(c[9]), uuid(c[11]), texto(c[13]), uuid(c[45]), texto(c[46]), decimal(c[24]), decimal(c[47]), decimal(c[50]),
                        decimal(c[48]), decimal(c[49]), instante(c[27]), texto(c[28]), texto(c[29]), instante(c[32]), texto(c[35]), texto(c[36]),
                        texto(c[38]), texto(c[39]), instante(c[43])));
            }
            return new PaginaRevisiones(revisiones, total);
        });
    }

    private List<?> ejecutar(final String procedimiento, final Map<String, Object> parametros) {
        try {
            return CancelacionRowSupport.ejecutar(entityManager, procedimiento, parametros);
        } catch (final PersistenceException exception) {
            throw DbCodeTranslator.traducir(exception, "grupo");
        }
    }

    private static List<String> acciones(final Object valor) {
        final String texto = texto(valor);
        return texto == null || texto.isBlank() ? List.of() : Arrays.stream(texto.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private static Map<String, Object> comando(final UUID revisionId, final String decision, final String motivo, final UUID usuarioId, final UUID correlationId) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idRevision", revisionId);
        parametros.put("codigoDecision", decision);
        parametros.put("motivo", motivo);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioId);
        return parametros;
    }

    private static Map<String, Object> bandeja(final UUID usuarioId, final int pagina, final int tamano, final boolean soloPendientes,
                                               final String estado, final UUID programaId) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idUsuarioEjecutor", usuarioId);
        parametros.put("numeroPagina", pagina);
        parametros.put("tamanoPagina", tamano);
        parametros.put("soloPendientes", soloPendientes ? 1 : 0);
        parametros.put("codigoEstado", estado == null || estado.isBlank() ? null : estado.strip());
        parametros.put("idPrograma", programaId);
        return parametros;
    }
}
