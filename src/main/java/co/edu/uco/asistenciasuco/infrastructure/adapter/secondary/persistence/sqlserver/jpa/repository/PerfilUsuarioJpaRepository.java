package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.PerfilUsuarioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.projection.PerfilUsuarioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.projection.VinculoUsuarioProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JPA del perfil del usuario autenticado (HU006). El SP devuelve una fila por vinculo con los datos personales
 * repetidos (o una sola fila con el vinculo en NULL); aqui se agrupan en un perfil.
 */
@Repository
public class PerfilUsuarioJpaRepository implements PerfilUsuarioQueryPort {

    static final String SQL_PERFIL = """
            EXEC dbo.usp_consultar_perfil_usuario
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String OP_PERFIL = "consultarPerfilUsuario";

    private final EntityManager entityManager;

    public PerfilUsuarioJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public Optional<PerfilUsuarioProjection> consultarPerfil(final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta del perfil es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_PERFIL,
                "No fue posible consultar el perfil del usuario desde base de datos.",
                () -> {
                    final List<Object[]> filas = ((List<?>) entityManager.createNativeQuery(SQL_PERFIL)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .getResultList()).stream()
                            .map(Object[].class::cast)
                            .toList();
                    if (filas.isEmpty()) {
                        return Optional.empty();
                    }
                    final Object[] f = filas.getFirst();
                    final List<VinculoUsuarioProjection> vinculos = filas.stream()
                            .filter(fila -> fila[12] != null)
                            .map(fila -> new VinculoUsuarioProjection(texto(fila[12]), (UUID) fila[13], texto(fila[14]),
                                    texto(fila[15]), Boolean.TRUE.equals(fila[16])))
                            .toList();
                    return Optional.of(new PerfilUsuarioProjection((UUID) f[0], texto(f[1]), texto(f[2]), entero(f[3]),
                            texto(f[4]), texto(f[5]), texto(f[6]), texto(f[7]), texto(f[8]), texto(f[9]),
                            Boolean.TRUE.equals(f[10]), Boolean.TRUE.equals(f[11]), vinculos));
                }
        );
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }
}
