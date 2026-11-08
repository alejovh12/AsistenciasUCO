package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaCsvParaSp;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador JPA de validacion de carga masiva de usuarios en SQL Server (CULM-001 D.5, CONTRACT §6.3, HU171).
 *
 * <p>Ejecuta {@code dbo.usp_validar_carga_masiva_usuarios} enviando las filas pre-parseadas como JSON
 * y devolviendo la proyeccion de validacion relacional por fila.</p>
 */
@Repository
public class CargaUsuariosJpaRepository implements CargaUsuariosValidacionPort {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    static final String OP_VALIDAR_CARGA = "validarCargaMasivaUsuarios";
    static final String SQL_VALIDAR_CARGA = """
            EXEC dbo.usp_validar_carga_masiva_usuarios
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @codigoPerfilDestino = :codigoPerfilDestino,
                 @filasJson = :filasJson
            """;

    private final EntityManager entityManager;

    public CargaUsuariosJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
    }

    @Override
    public List<FilaValidacionDbProjection> validarEnBaseDatos(
            final UUID idUsuarioEjecutor,
            final PerfilDestinoCarga perfilDestino,
            final List<FilaCsvParaSp> filas
    ) {
        if (idUsuarioEjecutor == null) {
            throw new CrosscuttingException("El idUsuarioEjecutor es obligatorio.");
        }
        if (perfilDestino == null) {
            throw new CrosscuttingException("El perfilDestino es obligatorio.");
        }

        final String filasJson = serializarFilas(filas);
        final UUID correlationId = CorrelationIdContext.get() != null ? CorrelationIdContext.get() : UUID.randomUUID();

        return JpaQueryExecutor.execute(
                OP_VALIDAR_CARGA,
                "No fue posible validar la carga masiva en base de datos.",
                () -> {
                    final List<?> resultados = entityManager.createNativeQuery(SQL_VALIDAR_CARGA)
                            .setParameter("idUsuarioEjecutor", idUsuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("codigoPerfilDestino", perfilDestino.name())
                            .setParameter("filasJson", filasJson)
                            .getResultList();

                    final List<FilaValidacionDbProjection> lista = new ArrayList<>();
                    for (final Object filaObj : resultados) {
                        final Object[] columnas = (Object[]) filaObj;
                        lista.add(new FilaValidacionDbProjection(
                                NativeRowValues.entero(columnas[0]),
                                NativeRowValues.bit(columnas[1]),
                                NativeRowValues.bit(columnas[2]),
                                NativeRowValues.texto(columnas[3]),
                                NativeRowValues.texto(columnas[4]),
                                NativeRowValues.uuid(columnas[5]),
                                NativeRowValues.uuid(columnas[6]),
                                NativeRowValues.uuid(columnas[7]),
                                NativeRowValues.uuid(columnas[8]),
                                NativeRowValues.uuid(columnas[9])
                        ));
                    }
                    return lista;
                }
        );
    }

    private static String serializarFilas(final List<FilaCsvParaSp> filas) {
        if (filas == null || filas.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(filas);
        } catch (final RuntimeException e) {
            throw new CrosscuttingException("Error serializando filas para SP de carga masiva: " + e.getMessage());
        }
    }
}
