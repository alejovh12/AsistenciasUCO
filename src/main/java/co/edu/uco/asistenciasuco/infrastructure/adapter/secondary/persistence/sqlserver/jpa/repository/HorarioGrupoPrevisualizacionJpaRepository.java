package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioGrupoPrevisualizacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection.Conflicto;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection.Festivo;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection.Franja;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection.Receso;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection.Resumen;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection.Sesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.FranjaHorarioRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * SES-GEN-01: adaptador JPA de {@code usp_previsualizar_horario_grupo} (solo lectura). El SP devuelve UN solo conjunto de resultados con
 * columnas fijas y filas discriminadas por {@code tipoFila}; se lee por posicion y con conteo estricto de columnas, igual que las demas
 * lecturas de SP. El significado de cada columna por tipo de fila esta en la cabecera del SP.
 */
@Repository
public class HorarioGrupoPrevisualizacionJpaRepository implements HorarioGrupoPrevisualizacionPort {

    static final String SQL_PREVISUALIZAR = """
            EXEC dbo.usp_previsualizar_horario_grupo
                 @idGrupo = :idGrupo,
                 @idAsignatura = :idAsignatura,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @idDocente = :idDocente,
                 @horariosJSON = :horariosJSON,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    /** tipoFila, orden, id1..id3, texto1..texto5, fecha1..fecha2, instante1..instante2, entero1..entero18, bandera1..bandera2, entero19..entero20 (DEC-081). */
    static final int COLUMNAS_PREVISUALIZACION = 36;

    private static final String OP_PREVISUALIZAR = "previsualizarHorarioGrupo";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final EntityManager entityManager;

    public HorarioGrupoPrevisualizacionJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public PrevisualizacionHorarioGrupoProjection previsualizar(final UUID idGrupo, final UUID idAsignatura, final UUID idPeriodoAcademico,
                                                                final UUID idDocente, final List<FranjaHorarioRepositoryDTO> franjas,
                                                                final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", idGrupo);
        parametros.put("idAsignatura", idAsignatura);
        parametros.put("idPeriodoAcademico", idPeriodoAcademico);
        parametros.put("idDocente", idDocente);
        parametros.put("horariosJSON", serializarFranjas(franjas));
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        return JpaQueryExecutor.execute(OP_PREVISUALIZAR, "No fue posible previsualizar el horario del grupo.", () -> {
            final List<?> filas;
            try {
                final var query = entityManager.createNativeQuery(SQL_PREVISUALIZAR);
                parametros.forEach(query::setParameter);
                filas = query.getResultList();
            } catch (final PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception);
            }
            return aProyeccion(filas);
        });
    }

    static PrevisualizacionHorarioGrupoProjection aProyeccion(final List<?> filas) {
        Resumen resumen = null;
        final List<Franja> franjas = new ArrayList<>();
        final List<Sesion> sesiones = new ArrayList<>();
        final List<Festivo> festivos = new ArrayList<>();
        final List<Receso> recesos = new ArrayList<>();
        final List<Conflicto> conflictos = new ArrayList<>();
        for (final Object fila : filas) {
            final Object[] c = exigirColumnas((Object[]) fila);
            switch ((String) c[0]) {
                case "RESUMEN" -> resumen = new Resumen(uuid(c[2]), uuid(c[3]), uuid(c[4]), (String) c[5], (String) c[6], (String) c[7], (String) c[8],
                        (String) c[9], fecha(c[10]), fecha(c[11]), entero(c[14]), nulo0(c[15]), nulo0(c[16]), nulo0(c[17]), nulo0(c[18]), nulo0(c[19]),
                        nulo0(c[20]), nulo0(c[21]), nulo0(c[22]), nulo0(c[23]), nulo0(c[24]), nulo0(c[25]), entero(c[26]), entero(c[27]), entero(c[28]),
                        entero(c[29]), entero(c[30]), entero(c[31]), bool(c[32]), bool(c[33]), nulo0(c[34]), nulo0(c[35]));
                case "FRANJA" -> franjas.add(new Franja((String) c[5], (String) c[6], (String) c[7], (String) c[8], nulo0(c[14])));
                case "SESION" -> sesiones.add(new Sesion(nulo0(c[14]), nulo0(c[15]), fecha(c[10]), (String) c[7], (String) c[8], instante(c[12]),
                        instante(c[13]), nulo0(c[16]), (String) c[5],
                        esReceso(c) ? null : (String) c[6], esReceso(c) ? (String) c[6] : null));
                case "FESTIVO" -> festivos.add(new Festivo(fecha(c[10]), (String) c[5], nulo0(c[14])));
                case "RECESO" -> recesos.add(new Receso(fecha(c[10]), (String) c[5], nulo0(c[14])));
                case "CONFLICTO" -> conflictos.add(new Conflicto((String) c[5], (String) c[6], (String) c[7], (String) c[8]));
                default -> throw new IllegalStateException("usp_previsualizar_horario_grupo devolvio un tipo de fila desconocido: " + c[0]);
            }
        }
        if (resumen == null) {
            throw new IllegalStateException("usp_previsualizar_horario_grupo no devolvio la fila RESUMEN.");
        }
        return new PrevisualizacionHorarioGrupoProjection(resumen, franjas, sesiones, festivos, conflictos, recesos);
    }

    private static boolean esReceso(final Object[] c) {
        return "CANCELADA_POR_RECESO".equals(c[5]);
    }

    private static Object[] exigirColumnas(final Object[] fila) {
        if (fila.length != COLUMNAS_PREVISUALIZACION) {
            throw new IllegalStateException("usp_previsualizar_horario_grupo debe devolver " + COLUMNAS_PREVISUALIZACION
                    + " columnas y devolvio " + fila.length + ".");
        }
        return fila;
    }

    private static String serializarFranjas(final List<FranjaHorarioRepositoryDTO> franjas) {
        try {
            final List<Map<String, String>> items = new ArrayList<>();
            for (final FranjaHorarioRepositoryDTO franja : franjas) {
                final Map<String, String> item = new LinkedHashMap<>();
                item.put("codigoDia", franja.codigoDia());
                item.put("horaInicio", franja.horaInicio() == null ? null : franja.horaInicio().format(HORA));
                item.put("horaFin", franja.horaFin() == null ? null : franja.horaFin().format(HORA));
                items.add(item);
            }
            return OBJECT_MAPPER.writeValueAsString(items);
        } catch (RuntimeException exception) {
            throw new CrosscuttingException("No fue posible serializar el horario propuesto del grupo.", exception);
        }
    }

    private static UUID uuid(final Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof UUID u ? u : UUID.fromString(valor.toString());
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    private static int nulo0(final Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private static boolean bool(final Object valor) {
        return valor instanceof Boolean b ? b : valor != null && Integer.parseInt(valor.toString()) != 0;
    }

    /** Columna DATE del SP: LocalDate, LocalDateTime o java.util.Date segun el driver; se recupera la fecha de pared. */
    private static LocalDate fecha(final Object valor) {
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor instanceof LocalDateTime fecha) {
            return fecha.toLocalDate();
        }
        if (valor instanceof Date fecha) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(fecha.getTime()), ZoneId.systemDefault());
        }
        return null;
    }

    /** Columna DATETIME2 en UTC (sin zona): Timestamp, LocalDateTime, Instant u OffsetDateTime segun el driver. */
    private static Instant instante(final Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Instant i) {
            return i;
        }
        if (valor instanceof OffsetDateTime o) {
            return o.toInstant();
        }
        if (valor instanceof LocalDateTime l) {
            return l.toInstant(ZoneOffset.UTC);
        }
        if (valor instanceof Date fecha) {
            // El driver arma el Timestamp con la hora de pared de la columna en la zona de la JVM: se recupera esa hora de pared (UTC en la DB).
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(fecha.getTime()), ZoneId.systemDefault()).toInstant(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Tipo de instante no soportado en usp_previsualizar_horario_grupo: " + valor.getClass().getName());
    }
}
