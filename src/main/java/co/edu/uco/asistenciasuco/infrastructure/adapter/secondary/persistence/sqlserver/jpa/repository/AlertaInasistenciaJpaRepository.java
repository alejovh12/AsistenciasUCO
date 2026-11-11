package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.*;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AlertaInasistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
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
 * CULM-001 C.1 (HU026C): motor de alertas, grupos pendientes, alertas del estudiante y resolutores via SP (JPA, {@code EXEC} nativo).
 * Los comandos de sistema ({@code usp_evaluar_alertas_inasistencia_grupo}, {@code usp_actualizar_resolutores_revisiones_cancelacion})
 * devuelven sus novedades como {@code novedadesJSON}.
 */
@Repository
public class AlertaInasistenciaJpaRepository implements AlertaInasistenciaRepositoryPort {

    static final String OP_EVALUAR = "evaluarAlertasInasistenciaGrupo";
    static final String OP_RESOLUTORES = "actualizarResolutoresRevisiones";
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AlertaInasistenciaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de alertas es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public ResultadoEvaluacion evaluar(final UUID grupoId, final UUID estudianteGrupoId) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", grupoId);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idEstudianteGrupo", estudianteGrupoId);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_EVALUAR,
                CancelacionRowSupport.exec("usp_evaluar_alertas_inasistencia_grupo", parametros), parametros, correlationId, 5);
        final List<Object> c = fila.extras();
        return new ResultadoEvaluacion(entero(c.get(0)), entero(c.get(1)), entero(c.get(2)), entero(c.get(3)), novedades(texto(c.get(4))));
    }

    @Override
    public List<UUID> consultarGruposPendientes(final Integer lote, final UUID periodoId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarGruposPendientesEvaluacionAlertas",
                "No fue posible consultar los grupos pendientes de evaluacion de alertas.", () -> {
                    final Map<String, Object> parametros = new LinkedHashMap<>();
                    parametros.put("idCorrelacion", correlationId);
                    parametros.put("tamanoLote", lote);
                    parametros.put("idPeriodoAcademico", periodoId);
                    final List<?> filas;
                    try {
                        filas = CancelacionRowSupport.ejecutar(entityManager, "usp_consultar_grupos_pendientes_evaluacion_alertas", parametros);
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception, "grupo");
                    }
                    final List<UUID> grupos = new ArrayList<>(filas.size());
                    for (final Object fila : filas) {
                        grupos.add(uuid(((Object[]) fila)[0]));
                    }
                    return grupos;
                });
    }

    @Override
    public PaginaAlertas consultarAlertasEstudiante(final UUID usuarioId, final int pagina, final int tamano, final boolean soloNoLeidas,
                                                    final boolean soloVigentes, final UUID grupoId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarAlertasEstudiante", "No fue posible consultar las alertas del estudiante.", () -> {
            final Map<String, Object> parametros = new LinkedHashMap<>();
            parametros.put("idUsuarioEjecutor", usuarioId);
            parametros.put("idCorrelacion", correlationId);
            parametros.put("numeroPagina", pagina);
            parametros.put("tamanoPagina", tamano);
            parametros.put("soloNoLeidas", soloNoLeidas ? 1 : 0);
            parametros.put("soloVigentes", soloVigentes ? 1 : 0);
            parametros.put("idGrupo", grupoId);
            final List<?> filas;
            try {
                filas = CancelacionRowSupport.ejecutar(entityManager, "usp_consultar_alertas_estudiante", parametros);
            } catch (final PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception, "grupo");
            }
            final List<AlertaFila> alertas = new ArrayList<>(filas.size());
            long total = 0;
            for (final Object fila : filas) {
                final Object[] c = (Object[]) fila;
                total = Math.max(total, entero(c[30]));
                alertas.add(new AlertaFila(uuid(c[0]), texto(c[12]), texto(c[13]), texto(c[17]), texto(c[15]), decimal(c[19]), decimal(c[28]),
                        decimal(c[20]), decimal(c[29]), decimal(c[21]), decimal(c[22]), instante(c[23]), bit(c[27]), instante(c[26]),
                        uuid(c[4]), entero(c[5]), texto(c[6]), uuid(c[7]), texto(c[8]), texto(c[10]), null));
            }
            return new PaginaAlertas(alertas, total);
        });
    }

    @Override
    public ResultadoResolutores actualizarResolutores(final Integer lote) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idCorrelacion", correlationId);
        parametros.put("tamanoLote", lote);
        final ProcedureRow fila = procedureExecutor.executeWithExtras(OP_RESOLUTORES,
                CancelacionRowSupport.exec("usp_actualizar_resolutores_revisiones_cancelacion", parametros),
                CancelacionRowSupport.sinNulos(parametros), correlationId, 4);
        final List<Object> c = fila.extras();
        return new ResultadoResolutores(entero(c.get(0)), entero(c.get(1)), entero(c.get(2)), novedadesResolutores(texto(c.get(3))));
    }

    // ------------------------------------------------------------------ JSON de las novedades

    static List<NovedadAlerta> novedades(final String json) {
        final List<NovedadAlerta> resultado = new ArrayList<>();
        for (final JsonNode n : leerArreglo(json)) {
            resultado.add(new NovedadAlerta(uuidJson(n.path("idAlerta")), n.path("codigoTipo").asString(""), n.path("nivel").asInt(0),
                    n.path("esPrincipal").asBoolean(false), decimalJson(n.path("porcentajeInasistencia")), decimalJson(n.path("umbral")),
                    decimalJson(n.path("umbralPerdida")), decimalJson(n.path("horasFalta")), decimalJson(n.path("horasDictadas")), uuidJson(n.path("idEstudianteGrupo")),
                    uuidJson(n.path("idGrupo")), n.path("codigoGrupo").asInt(0), n.path("nombreGrupo").asString(""),
                    n.path("nombreAsignatura").asString(""), n.path("tipoPlantillaEstudiante").asString(""),
                    persona(n.path("estudiante")), persona(n.path("docente")), uuidJson(n.path("idRevisionCreada"))));
        }
        return List.copyOf(resultado);
    }

    static List<NovedadResolutores> novedadesResolutores(final String json) {
        final List<NovedadResolutores> resultado = new ArrayList<>();
        for (final JsonNode n : leerArreglo(json)) {
            final List<Resolutor> resolutores = new ArrayList<>();
            for (final JsonNode r : n.path("resolutores")) {
                resolutores.add(new Resolutor(uuidJson(r.path("idUsuario")), r.path("nombreCompleto").asString(""), r.path("correo").asString(null),
                        r.path("codigoPerfil").asString(""), r.path("esEscalamiento").asBoolean(false)));
            }
            resultado.add(new NovedadResolutores(uuidJson(n.path("idRevision")), uuidJson(n.path("idGrupo")), n.path("nombreGrupo").asString(""),
                    n.path("nombreAsignatura").asString(""), uuidJson(n.path("idEstudianteGrupo")), n.path("perfilResolutor").asString(null),
                    n.path("evento").asString(""), decimalJson(n.path("porcentajeInasistencia")), decimalJson(n.path("umbral")),
                    n.path("motivo").asString(""), n.path("nombreCompletoEstudiante").asString(""), n.path("nombreCompletoDocente").asString(""),
                    uuidJson(n.path("idUsuarioDocente")), uuidJson(n.path("idUsuarioEstudiante")), List.copyOf(resolutores)));
        }
        return List.copyOf(resultado);
    }

    private static Persona persona(final JsonNode nodo) {
        if (nodo.isMissingNode() || nodo.isNull()) {
            return null;
        }
        return new Persona(uuidJson(nodo.path("idUsuario")), nodo.path("nombreCompleto").asString(""), nodo.path("correo").asString(null));
    }

    private static JsonNode leerArreglo(final String json) {
        if (json == null || json.isBlank()) {
            return MAPPER.createArrayNode();
        }
        try {
            final JsonNode raiz = MAPPER.readTree(json);
            if (!raiz.isArray()) {
                throw new DatabaseOperationException("El JSON de novedades no es un arreglo.", new IllegalStateException("novedadesJSON"));
            }
            return raiz;
        } catch (final JacksonException exception) {
            throw new DatabaseOperationException("El JSON de novedades no es valido.", exception);
        }
    }

    private static UUID uuidJson(final JsonNode nodo) {
        if (nodo.isMissingNode() || nodo.isNull() || nodo.asString("").isBlank()) {
            return null;
        }
        return UUID.fromString(nodo.asString().strip());
    }

    private static BigDecimal decimalJson(final JsonNode nodo) {
        return nodo.isMissingNode() || nodo.isNull() ? null : new BigDecimal(nodo.asString());
    }
}
