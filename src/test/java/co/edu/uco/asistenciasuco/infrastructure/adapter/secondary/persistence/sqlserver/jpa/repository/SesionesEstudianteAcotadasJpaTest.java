package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaSesionEstudianteQueryRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PAG-EST-SESIONES (RED/GREEN): las lecturas acotadas del estudiante ejecutan HQL con {@code in :idsSesion}, conservan el
 * aislamiento por estudiante/asignatura, no ejecutan nada con ids vacios y trocean listas grandes bajo el tope de
 * parametros de SQL Server. La prueba contra SQL Server real vive en los IT del mismo paquete.
 */
@SuppressWarnings("unchecked")
class SesionesEstudianteAcotadasJpaTest {

    private final EntityManager manager = mock(EntityManager.class);
    private final UUID estudiante = UUID.randomUUID();
    private final UUID asignatura = UUID.randomUUID();

    private TypedQuery<AsistenciaSesionEstudianteQueryRow> consultaAsistencia() {
        final TypedQuery<AsistenciaSesionEstudianteQueryRow> query = mock(TypedQuery.class, RETURNS_SELF);
        when(query.getResultList()).thenReturn(List.of());
        when(manager.createQuery(AsistenciaSesionEstudianteJpaRepository.HQL_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES,
                AsistenciaSesionEstudianteQueryRow.class)).thenReturn(query);
        return query;
    }

    private static List<UUID> ids(final int cantidad) {
        return IntStream.range(0, cantidad).mapToObj(i -> UUID.randomUUID()).toList();
    }

    @Test
    @DisplayName("asistencia acotada: filtra por estudiante, asignatura y los ids de la pagina en una sola consulta")
    void asistencia_acotada_envia_estudiante_asignatura_e_ids() {
        final var query = consultaAsistencia();
        final List<UUID> pagina = ids(3);

        new AsistenciaSesionEstudianteJpaRepository(manager).consultarAsistenciaSesiones(estudiante, asignatura, pagina);

        verify(query).setParameter("idEstudiante", estudiante);
        verify(query).setParameter("idAsignatura", asignatura);
        verify(query).setParameter("idsSesion", pagina);
        verify(manager, times(1)).createQuery(any(String.class), eq(AsistenciaSesionEstudianteQueryRow.class));
        assertTrue(AsistenciaSesionEstudianteJpaRepository.HQL_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES
                .contains("eg.idEstudiante = :idEstudiante"));
        assertTrue(AsistenciaSesionEstudianteJpaRepository.HQL_POR_ESTUDIANTE_ASIGNATURA_Y_SESIONES
                .contains("a.idSesion in :idsSesion"));
    }

    @Test
    @DisplayName("asistencia acotada: ids nulos, vacios o repetidos no generan consultas innecesarias")
    void asistencia_acotada_sin_ids_no_consulta_y_deduplica() {
        final var repo = new AsistenciaSesionEstudianteJpaRepository(manager);

        assertTrue(repo.consultarAsistenciaSesiones(estudiante, asignatura, List.of()).isEmpty());
        assertTrue(repo.consultarAsistenciaSesiones(estudiante, asignatura, null).isEmpty());
        verify(manager, never()).createQuery(any(String.class), eq(AsistenciaSesionEstudianteQueryRow.class));

        final var query = consultaAsistencia();
        final UUID repetido = UUID.randomUUID();
        final List<UUID> conRepetidos = new ArrayList<>(List.of(repetido, repetido));
        repo.consultarAsistenciaSesiones(estudiante, asignatura, conRepetidos);
        verify(query).setParameter("idsSesion", List.of(repetido));
    }

    @Test
    @DisplayName("asistencia acotada: una lista de 2500 ids se trocea en bloques de 1000 y se unen los resultados")
    void asistencia_acotada_trocea_listas_grandes() {
        final var query = consultaAsistencia();
        final UUID sesion = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(
                new AsistenciaSesionEstudianteQueryRow(sesion, 1, Boolean.TRUE, "AN", null, null, null)));

        final var resultado = new AsistenciaSesionEstudianteJpaRepository(manager)
                .consultarAsistenciaSesiones(estudiante, asignatura, ids(2500));

        final ArgumentCaptor<Object> parametros = ArgumentCaptor.forClass(Object.class);
        verify(query, times(3)).setParameter(eq("idsSesion"), parametros.capture());
        final List<Integer> tamanos = parametros.getAllValues().stream().map(v -> ((List<?>) v).size()).toList();
        assertEquals(List.of(1000, 1000, 500), tamanos);
        assertEquals(3, resultado.size());
    }

    @Test
    @DisplayName("procedencia por ids: HQL usa 'in :idsSesion' ordenado y no filtra por grupo completo")
    void procedencia_por_ids_usa_in_y_no_lee_el_grupo() {
        assertTrue(SesionProcedenciaJpaRepository.HQL_POR_IDS.contains("s.id in :idsSesion"));
        assertTrue(!SesionProcedenciaJpaRepository.HQL_POR_IDS.contains("idGrupo"));
        assertTrue(SesionProcedenciaJpaRepository.HQL_POR_IDS.contains("order by s.fechaHoraInicio, s.numero, s.id"));

        final TypedQuery<Object[]> query = mock(TypedQuery.class, RETURNS_SELF);
        when(query.getResultList()).thenReturn(List.of());
        when(manager.createQuery(SesionProcedenciaJpaRepository.HQL_POR_IDS, Object[].class)).thenReturn(query);
        final var repo = new SesionProcedenciaJpaRepository(manager);

        assertTrue(repo.consultarSesionesPorIds(List.of()).isEmpty());
        verify(manager, never()).createQuery(any(String.class), eq(Object[].class));

        repo.consultarSesionesPorIds(ids(1001));
        verify(query, times(2)).setParameter(eq("idsSesion"), any(List.class));
    }
}
