package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ATA D7: calendario de sesiones (horario semanal con fechas) via {@code usp_consultar_sesiones_calendario_estudiante} y
 * {@code usp_consultar_sesiones_calendario_docente} (JPA, {@code EXEC} nativo, solo lectura). Los dos SP devuelven las mismas 16
 * columnas de {@code uv_sesion_calendario}; el perfil ({@code SEC_001}) y el rango ({@code CAL_001}) los valida la DB.
 */
@Repository
public class SesionCalendarioJpaRepository implements SesionCalendarioRepositoryPort {

    static final String SQL_ESTUDIANTE = """
            EXEC dbo.usp_consultar_sesiones_calendario_estudiante
                 @idUsuario = :idUsuario, @desde = :desde, @hasta = :hasta, @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_DOCENTE = """
            EXEC dbo.usp_consultar_sesiones_calendario_docente
                 @idUsuario = :idUsuario, @desde = :desde, @hasta = :hasta, @idCorrelacion = :idCorrelacion
            """;
    /** idSesion, idGrupo, codigoGrupo, nombreGrupo, idAsignatura, codigoAsignatura, nombreAsignatura, idDocente, nombreDocente, inicio, fin, 5 banderas/textos. */
    static final int COLUMNAS_CALENDARIO = 16;

    private final EntityManager entityManager;

    public SesionCalendarioJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del calendario de sesiones es obligatorio.");
    }

    @Override
    public List<SesionCalendario> consultarEstudiante(final UUID usuarioId, final LocalDate desde, final LocalDate hasta) {
        return consultar("consultarCalendarioEstudiante", SQL_ESTUDIANTE, usuarioId, desde, hasta);
    }

    @Override
    public List<SesionCalendario> consultarDocente(final UUID usuarioId, final LocalDate desde, final LocalDate hasta) {
        return consultar("consultarCalendarioDocente", SQL_DOCENTE, usuarioId, desde, hasta);
    }

    private List<SesionCalendario> consultar(final String operacion, final String sql, final UUID usuarioId, final LocalDate desde,
                                             final LocalDate hasta) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                operacion,
                "No fue posible consultar el calendario de sesiones.",
                () -> ((List<?>) entityManager.createNativeQuery(sql)
                        .setParameter("idUsuario", usuarioId)
                        .setParameter("desde", desde)
                        .setParameter("hasta", hasta)
                        .setParameter("idCorrelacion", correlationId)
                        .getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(SesionCalendarioJpaRepository::aSesion)
                        .toList(),
                DbCodeTranslator::traducir
        );
    }

    private static SesionCalendario aSesion(final Object[] f) {
        if (f.length != COLUMNAS_CALENDARIO) {
            throw new IllegalStateException("El SP del calendario de sesiones devolvio " + f.length + " columnas y se esperaban "
                    + COLUMNAS_CALENDARIO + ".");
        }
        return new SesionCalendario(
                NativeRowValues.uuid(f[0]), NativeRowValues.uuid(f[1]), f[2] == null ? null : NativeRowValues.entero(f[2]), texto(f[3]),
                NativeRowValues.uuid(f[4]), texto(f[5]), texto(f[6]), NativeRowValues.uuid(f[7]), texto(f[8]),
                SesionCancelacionJpaRepository.fechaHora(f[9]), SesionCancelacionJpaRepository.fechaHora(f[10]),
                CancelacionRowSupport.bit(f[11]), CancelacionRowSupport.bit(f[12]), texto(f[13]), CancelacionRowSupport.bit(f[14]),
                texto(f[15]));
    }

    private static String texto(final Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
