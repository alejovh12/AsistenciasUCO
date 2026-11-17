package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Repositorio JPA directo consolidado para Coordinador. */
@Repository
public class CoordinadorJpaRepository implements CoordinadorQueryPort, CoordinadorCommandPort {
    private static final String PARAM_ID_COORDINADOR = "idCoordinador";
    private static final String PARAM_PRIMER_NOMBRE = "primerNombre";
    private static final String PARAM_SEGUNDO_NOMBRE = "segundoNombre";
    private static final String PARAM_PRIMER_APELLIDO = "primerApellido";
    private static final String PARAM_SEGUNDO_APELLIDO = "segundoApellido";
    private static final String PARAM_CORREO = "correo";
    private static final String PARAM_ID_FACULTAD = "idFacultad";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";

    static final String SQL_CREAR_COORDINADOR = """
            EXEC dbo.usp_crear_coordinador
                 @idCoordinador = :idCoordinador,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idPrograma = :idPrograma,
                 @idFacultad = :idFacultad,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion
            """;

    // DEC-065 (5): una sola transaccion de BD para persona + rol + programa. Resultset canonico + idCoordinador, idPrograma, nombrePrograma.
    static final String SQL_CREAR_COORDINADOR_CON_PROGRAMA = """
            EXEC dbo.usp_crear_coordinador_con_programa
                 @idCoordinador = :idCoordinador,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @password = :password,
                 @nombrePrograma = :nombrePrograma,
                 @idTipoPrograma = :idTipoPrograma,
                 @idModalidad = :idModalidad,
                 @idJornada = :idJornada,
                 @idFacultad = :idFacultad,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion
            """;

    static final String SQL_ACTUALIZAR_COORDINADOR = """
            EXEC dbo.usp_actualizar_coordinador
                 @idCoordinador = :idCoordinador,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idFacultad = :idFacultad,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @motivo = :motivo
            """;

    static final String SQL_CAMBIAR_ESTADO_COORDINADOR = """
            EXEC dbo.usp_cambiar_estado_coordinador
                 @idCoordinador = :idCoordinador,
                 @estaActivo = :estaActivo,
                 @idFacultad = :idFacultad,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    /**
     * CIERRE-W1B: coordinadores de la facultad del decano ejecutor con filtros y paginacion en el SP (el ambito y el
     * perfil DECANO los valida el SP). Columnas: id, idUsuario, numeroIdentificacion, nombreCompleto, idPrograma,
     * nombrePrograma, estaActivoCoordinador, primerNombre, segundoNombre, primerApellido, segundoApellido, correo,
     * estaActivoRolCoordinador, nombreFacultad, totalDocentes, totalGrupos, totalRegistros.
     */
    static final String SQL_CONSULTAR_COORDINADORES_FACULTAD = """
            EXEC dbo.usp_consultar_coordinadores_facultad_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @busqueda = :busqueda,
                 @estaActivo = :estaActivo,
                 @sinPrograma = :sinPrograma
            """;

    static final String OP_CREAR_COORDINADOR = "crearCoordinador";
    static final String OP_CREAR_COORDINADOR_CON_PROGRAMA = "crearCoordinadorConPrograma";
    static final String OP_ACTUALIZAR_COORDINADOR = "actualizarCoordinador";
    static final String OP_CAMBIAR_ESTADO_COORDINADOR = "cambiarEstadoCoordinador";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public CoordinadorJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void crearCoordinador(final UUID idCoordinador, final String numeroIdentificacion,
                                 final String primerNombre, final String segundoNombre,
                                 final String primerApellido, final String segundoApellido, final String correo,
                                 final UUID idPrograma, final UUID idFacultad, final String password,
                                 final UUID usuarioEjecutor, final UUID idTipoIdIdentificacion) {
        ejecutarCrearCoordinador(idCoordinador, numeroIdentificacion, primerNombre, segundoNombre,
                primerApellido, segundoApellido, correo, idPrograma, idFacultad, password, usuarioEjecutor, idTipoIdIdentificacion);
    }

    @Override
    public ResultadoCoordinadorConPrograma crearCoordinadorConPrograma(final CrearCoordinadorConProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_COORDINADOR, command.idCoordinador());
        parametros.put("numeroIdentificacion", command.numeroIdentificacion());
        parametros.put(PARAM_PRIMER_NOMBRE, command.primerNombre());
        parametros.put(PARAM_SEGUNDO_NOMBRE, command.segundoNombre());
        parametros.put(PARAM_PRIMER_APELLIDO, command.primerApellido());
        parametros.put(PARAM_SEGUNDO_APELLIDO, command.segundoApellido());
        parametros.put(PARAM_CORREO, command.correo());
        parametros.put("password", command.password());
        parametros.put("nombrePrograma", command.nombrePrograma());
        parametros.put("idTipoPrograma", command.idTipoPrograma());
        parametros.put("idModalidad", command.idModalidad());
        parametros.put("idJornada", command.idJornada());
        parametros.put(PARAM_ID_FACULTAD, command.idFacultad());
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        parametros.put(PARAM_ID_USUARIO_EJECUTOR, command.usuarioEjecutor());
        parametros.put("idTipoIdIdentificacion", command.tipoIdentificacionId());

        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CREAR_COORDINADOR_CON_PROGRAMA, SQL_CREAR_COORDINADOR_CON_PROGRAMA, parametros, correlationId, 3);
        return new ResultadoCoordinadorConPrograma(
                idObligatorio(fila.extras().get(0), PARAM_ID_COORDINADOR),
                idObligatorio(fila.extras().get(1), "idPrograma"),
                NativeRowValues.texto(fila.extras().get(2)));
    }

    private static UUID idObligatorio(final Object valor, final String columna) {
        try {
            final UUID id = NativeRowValues.uuid(valor);
            if (id != null) {
                return id;
            }
        } catch (final IllegalArgumentException exception) {
            throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El procedimiento " + OP_CREAR_COORDINADOR_CON_PROGRAMA + " retorno " + columna + " con formato invalido.", exception);
        }
        throw new DatabaseOperationException(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                "El procedimiento " + OP_CREAR_COORDINADOR_CON_PROGRAMA + " no retorno " + columna + ".");
    }

    @Override
    public ResultadoGestionCoordinadorProjection actualizarCoordinador(
            final UUID idCoordinador, final String primerNombre, final String segundoNombre,
            final String primerApellido, final String segundoApellido, final String correo,
            final UUID idFacultad, final UUID usuarioEjecutor, final CorreccionDocumentoDTO documento) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_COORDINADOR, idCoordinador);
        parametros.put(PARAM_PRIMER_NOMBRE, primerNombre);
        parametros.put(PARAM_SEGUNDO_NOMBRE, segundoNombre);
        parametros.put(PARAM_PRIMER_APELLIDO, primerApellido);
        parametros.put(PARAM_SEGUNDO_APELLIDO, segundoApellido);
        parametros.put(PARAM_CORREO, correo);
        parametros.put(PARAM_ID_FACULTAD, idFacultad);
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        parametros.put(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor);
        CorreccionDocumentoParametros.agregar(parametros, documento);

        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_ACTUALIZAR_COORDINADOR, SQL_ACTUALIZAR_COORDINADOR, parametros, correlationId, 1);
        return new ResultadoGestionCoordinadorProjection(idUsuarioDeFila(fila, OP_ACTUALIZAR_COORDINADOR), false);
    }

    @Override
    public ResultadoGestionCoordinadorProjection cambiarEstadoCoordinador(
            final UUID idCoordinador, final boolean estaActivo, final UUID idFacultad, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_COORDINADOR, idCoordinador);
        parametros.put("estaActivo", estaActivo);
        parametros.put(PARAM_ID_FACULTAD, idFacultad);
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        parametros.put(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor);

        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CAMBIAR_ESTADO_COORDINADOR, SQL_CAMBIAR_ESTADO_COORDINADOR, parametros, correlationId, 2);
        return new ResultadoGestionCoordinadorProjection(
                idUsuarioDeFila(fila, OP_CAMBIAR_ESTADO_COORDINADOR), flag(fila.extras().get(1)));
    }

    // QUERIES

    @Override
    public Pagina<CoordinadorFacultadProjection> consultarCoordinadoresFacultad(
            final UUID idUsuarioEjecutor, final String busqueda, final Boolean estaActivo, final Boolean sinPrograma,
            final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarCoordinadoresFacultad",
                "No fue posible consultar los coordinadores de la facultad.",
                () -> {
                    final List<?> filas;
                    try {
                        filas = entityManager.createNativeQuery(SQL_CONSULTAR_COORDINADORES_FACULTAD)
                                .setParameter(PARAM_ID_USUARIO_EJECUTOR, idUsuarioEjecutor)
                                .setParameter(PARAM_ID_CORRELACION, correlationId)
                                .setParameter("numeroPagina", pagina)
                                .setParameter("tamanoPagina", tamano)
                                .setParameter("busqueda", busqueda)
                                .setParameter("estaActivo", estaActivo)
                                .setParameter("sinPrograma", sinPrograma)
                                .getResultList();
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception);
                    }
                    final List<CoordinadorFacultadProjection> elementos = new java.util.ArrayList<>();
                    int total = 0;
                    for (final Object fila : filas) {
                        final Object[] c = (Object[]) fila;
                        elementos.add(new CoordinadorFacultadProjection(
                                NativeRowValues.uuid(c[0]), NativeRowValues.uuid(c[1]), NativeRowValues.texto(c[2]),
                                NativeRowValues.texto(c[3]), NativeRowValues.uuid(c[4]), NativeRowValues.texto(c[5]),
                                NativeRowValues.bit(c[6]), NativeRowValues.texto(c[7]), NativeRowValues.texto(c[8]),
                                NativeRowValues.texto(c[9]), NativeRowValues.texto(c[10]), NativeRowValues.texto(c[11]),
                                NativeRowValues.bit(c[12]), NativeRowValues.texto(c[13]), NativeRowValues.entero(c[14]),
                                NativeRowValues.entero(c[15])));
                        total = NativeRowValues.entero(c[16]);
                    }
                    return new Pagina<>(elementos, total);
                }
        );
    }

    // PRIVATE HELPERS

    /** El SP devuelve el usuario del coordinador cuando la operacion tuvo exito; su ausencia es una violacion del contrato. */
    private static UUID idUsuarioDeFila(final ProcedureRow fila, final String operation) {
        final Object valor = fila.extras().getFirst();
        if (valor instanceof UUID uuid) {
            return uuid;
        }
        // SQL Server devuelve uniqueidentifier como texto en las consultas nativas (igual que la correlacion canonica).
        if (valor instanceof String texto) {
            try {
                return UUID.fromString(texto.trim());
            } catch (IllegalArgumentException exception) {
                throw new DatabaseOperationException(
                        DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                        "El procedimiento " + operation + " retorno un usuario con formato invalido.", exception);
            }
        }
        throw new DatabaseOperationException(
                DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                "El procedimiento " + operation + " no retorno el usuario del coordinador.");
    }

    private static boolean flag(final Object valor) {
        if (valor instanceof Boolean bit) {
            return bit;
        }
        return valor instanceof Number numero && numero.intValue() == 1;
    }

    private ProcedureResult ejecutarCrearCoordinador(
            final UUID idCoordinador,
            final String numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idPrograma,
            final UUID idFacultad,
            final String password,
            final UUID usuarioEjecutor,
            final UUID idTipoIdIdentificacion
    ) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_COORDINADOR, idCoordinador);
        parametros.put("numeroIdentificacion", numeroIdentificacion);
        parametros.put(PARAM_PRIMER_NOMBRE, primerNombre);
        parametros.put(PARAM_SEGUNDO_NOMBRE, segundoNombre);
        parametros.put(PARAM_PRIMER_APELLIDO, primerApellido);
        parametros.put(PARAM_SEGUNDO_APELLIDO, segundoApellido);
        parametros.put(PARAM_CORREO, correo);
        parametros.put("idPrograma", idPrograma);
        parametros.put(PARAM_ID_FACULTAD, idFacultad);
        parametros.put("password", password);
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        parametros.put(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor);
        parametros.put("idTipoIdIdentificacion", idTipoIdIdentificacion);

        return procedureExecutor.execute(OP_CREAR_COORDINADOR, SQL_CREAR_COORDINADOR, parametros, correlationId);
    }
}
