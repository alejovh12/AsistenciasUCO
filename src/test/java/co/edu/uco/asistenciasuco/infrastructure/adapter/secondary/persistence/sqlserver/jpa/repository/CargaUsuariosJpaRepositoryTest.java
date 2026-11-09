package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaCsvParaSp;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.dto.FilaValidacionDbProjection;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.5 (CONTRACT 6.3, HU171): validacion relacional de la carga masiva via {@code usp_validar_carga_masiva_usuarios}. */
class CargaUsuariosJpaRepositoryTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private CargaUsuariosJpaRepository repo;

    @BeforeEach
    void preparar() {
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        repo = new CargaUsuariosJpaRepository(manager);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static FilaCsvParaSp fila() {
        return new FilaCsvParaSp(2, "CC", "1017123456", "ana@uco.edu.co", "TC", "ING-SIS", null);
    }

    @Test
    void valida_enviando_perfil_correlacion_y_filas_como_json_y_mapea_las_10_columnas() {
        CorrelationIdContext.set(CORRELACION);
        final UUID tipoId = UUID.randomUUID();
        final UUID vinculacion = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID existente = UUID.randomUUID();
        when(query.getResultList()).thenReturn(filas(
                new Object[]{2, true, 0, "CMA_OK", "Fila valida", tipoId.toString(), vinculacion.toString(), programa.toString(), null, null},
                new Object[]{3, 0, 1, "CMA_005", "Ya existe", null, null, null, null, existente}));

        final List<FilaValidacionDbProjection> filas = repo.validarEnBaseDatos(ACTOR, PerfilDestinoCarga.DOCENTE, List.of(fila()));

        assertEquals(2, filas.size());
        final FilaValidacionDbProjection primera = filas.getFirst();
        assertEquals(2, primera.numeroFila());
        assertTrue(primera.esValida());
        assertFalse(primera.yaExiste());
        assertEquals("CMA_OK", primera.codigoMensaje());
        assertEquals(tipoId, primera.idTipoIdentificacion());
        assertEquals(programa, primera.idPrograma());
        assertNull(primera.idFacultad());
        final FilaValidacionDbProjection segunda = filas.get(1);
        assertFalse(segunda.esValida());
        assertTrue(segunda.yaExiste());
        assertEquals(existente, segunda.idUsuarioExistente());
        verify(manager).createNativeQuery(CargaUsuariosJpaRepository.SQL_VALIDAR_CARGA);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("codigoPerfilDestino", "DOCENTE");
        final ArgumentCaptor<Object> json = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(org.mockito.ArgumentMatchers.eq("filasJson"), json.capture());
        assertTrue(json.getValue().toString().contains("\"numeroDocumento\":\"1017123456\""));
        assertTrue(json.getValue().toString().startsWith("["));
    }

    @Test
    void sin_correlacion_en_contexto_genera_una_y_sin_filas_envia_arreglo_vacio() {
        CorrelationIdContext.clear();
        when(query.getResultList()).thenReturn(List.of());

        assertTrue(repo.validarEnBaseDatos(ACTOR, PerfilDestinoCarga.DECANO, List.of()).isEmpty());
        assertTrue(repo.validarEnBaseDatos(ACTOR, PerfilDestinoCarga.DECANO, null).isEmpty());

        verify(query, org.mockito.Mockito.times(2)).setParameter("filasJson", "[]");
        verify(query, org.mockito.Mockito.times(2)).setParameter(org.mockito.ArgumentMatchers.eq("idCorrelacion"), any(UUID.class));
    }

    @Test
    void exige_ejecutor_y_perfil_antes_de_tocar_la_db() {
        assertThrows(CrosscuttingException.class, () -> repo.validarEnBaseDatos(null, PerfilDestinoCarga.DOCENTE, List.of()));
        assertThrows(CrosscuttingException.class, () -> repo.validarEnBaseDatos(ACTOR, null, List.of()));
        verify(manager, never()).createNativeQuery(anyString());
        assertThrows(NullPointerException.class, () -> new CargaUsuariosJpaRepository(null));
    }

    @Test
    void un_fallo_de_persistencia_es_database_operation_exception() {
        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.validarEnBaseDatos(ACTOR, PerfilDestinoCarga.COORDINADOR, List.of(fila())));
    }

    /** Lista de filas nativas: evita que {@code List.of} expanda el arreglo de columnas como varargs. */
    private static List<Object> filas(final Object[]... filas) {
        return new java.util.ArrayList<>(java.util.Arrays.asList((Object[]) filas));
    }
}
