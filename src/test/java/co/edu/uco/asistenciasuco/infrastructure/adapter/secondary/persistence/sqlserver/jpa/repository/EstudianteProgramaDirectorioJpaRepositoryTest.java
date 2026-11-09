package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaEstudiantesProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeAudienciaQueryPort.Audiencias;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B2NM / C.1: directorio paginado de estudiantes por programa y audiencias del SSE, ambos via SP. */
class EstudianteProgramaDirectorioJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] filaDirectorio(final Object id, final Object total) {
        return new Object[]{id, UUID.randomUUID().toString(), UUID.randomUUID(), 1017123456L, "Perez", "Gomez", "Ana", null, "Ana Perez Gomez", "ana@uco.edu.co", 1, total};
    }

    @Test
    void directorio_ejecuta_el_sp_con_ambito_filtros_y_pagina_y_mapea_12_columnas() {
        final UUID id = UUID.randomUUID();
        when(query.getResultList()).thenReturn(filas(filaDirectorio(id.toString(), 8L), filaDirectorio(UUID.randomUUID(), 8)));
        final EstudianteProgramaJpaRepository repo = new EstudianteProgramaJpaRepository(manager);

        final PaginaEstudiantesProgramaProjection pagina = repo.consultarDirectorioPrograma(ACTOR, PROGRAMA, "ana", Boolean.TRUE, 2, 5);

        assertEquals(8, pagina.total());
        assertEquals(2, pagina.elementos().size());
        final EstudianteProgramaProjection primera = pagina.elementos().getFirst();
        assertEquals(id, primera.id());
        assertEquals("1017123456", primera.numeroIdentificacion());
        assertEquals("Ana Perez Gomez", primera.nombreCompleto());
        assertEquals("ana@uco.edu.co", primera.correo());
        assertEquals(PROGRAMA, primera.idPrograma());
        assertNull(primera.nombrePrograma());
        verify(manager).createNativeQuery(EstudianteProgramaJpaRepository.SQL_DIRECTORIO);
        assertTrue(EstudianteProgramaJpaRepository.SQL_DIRECTORIO.contains("usp_consultar_directorio_estudiantes_paginado"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("nombre", "ana");
        verify(query).setParameter("activo", Boolean.TRUE);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
    }

    @Test
    void directorio_sin_filas_tiene_total_cero_y_con_valores_nulos_los_deja_nulos() {
        final EstudianteProgramaJpaRepository repo = new EstudianteProgramaJpaRepository(manager);
        when(query.getResultList()).thenReturn(List.of());
        assertEquals(0, repo.consultarDirectorioPrograma(ACTOR, PROGRAMA, null, null, 1, 10).total());

        final Object[] nulos = new Object[]{null, null, null, null, null, null, null, null, null, null, null, 1L};
        when(query.getResultList()).thenReturn(filas(nulos));
        final EstudianteProgramaProjection fila = repo.consultarDirectorioPrograma(ACTOR, PROGRAMA, null, null, 1, 10).elementos().getFirst();
        assertNull(fila.id());
        assertNull(fila.numeroIdentificacion());
        assertNull(fila.nombreCompleto());
        assertNull(fila.correo());
    }

    @Test
    void directorio_traduce_rechazos_y_encapsula_fallos_y_columnas_erradas() {
        final EstudianteProgramaJpaRepository repo = new EstudianteProgramaJpaRepository(manager);
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> repo.consultarDirectorioPrograma(ACTOR, PROGRAMA, null, null, 1, 10));

        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultarDirectorioPrograma(ACTOR, PROGRAMA, null, null, 1, 10));

        org.mockito.Mockito.reset(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(filas(new Object[11]));
        assertThrows(DatabaseOperationException.class, () -> repo.consultarDirectorioPrograma(ACTOR, PROGRAMA, null, null, 1, 10));
        assertThrows(NullPointerException.class, () -> new EstudianteProgramaJpaRepository(null));
    }

    // ---- audiencias realtime ----

    @Test
    void audiencias_mapea_los_tres_bits_de_la_primera_fila() {
        when(query.getResultList()).thenReturn(filas(new Object[]{1, false, true}));
        final RealtimeAudienciaJpaRepository repo = new RealtimeAudienciaJpaRepository(manager);

        final Audiencias audiencias = repo.consultar(ACTOR, GRUPO);

        assertEquals(new Audiencias(true, false, true), audiencias);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idGrupo", GRUPO);
    }

    @Test
    void audiencias_sin_filas_es_ninguna_audiencia() {
        when(query.getResultList()).thenReturn(List.of());
        final Audiencias audiencias = new RealtimeAudienciaJpaRepository(manager).consultar(ACTOR, GRUPO);
        assertFalse(audiencias.esDocenteTitular());
        assertFalse(audiencias.esEstudianteActivo());
        assertFalse(audiencias.esAmbitoLectura());
    }

    @Test
    void audiencias_traduce_rechazos_y_encapsula_fallos_tecnicos() {
        final RealtimeAudienciaJpaRepository repo = new RealtimeAudienciaJpaRepository(manager);
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> repo.consultar(ACTOR, GRUPO));

        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultar(ACTOR, GRUPO));
        assertThrows(NullPointerException.class, () -> new RealtimeAudienciaJpaRepository(null));
    }

    /** Lista de filas nativas: evita que {@code List.of} expanda el arreglo de columnas como varargs. */
    private static List<Object> filas(final Object[]... filas) {
        return new java.util.ArrayList<>(java.util.Arrays.asList((Object[]) filas));
    }
}
