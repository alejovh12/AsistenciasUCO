package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarCategoriaJustificacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Migracion 65: CategoriaJustificacionAdminJpaRepository (vista de administracion y SP usp_crear/actualizar/cambiar_estado_categoria_justificacion). */
class CategoriaJustificacionAdminJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private CategoriaJustificacionAdminJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new CategoriaJustificacionAdminJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] exito(final String codigo) {
        return new Object[]{CORRELATION, "Listo.", "DBCODE=" + codigo + "|", true};
    }

    private static Object[] rechazo(final String codigo) {
        return new Object[]{CORRELATION, "Rechazado.", "DBCODE=" + codigo + "|detalle", false};
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new CategoriaJustificacionAdminJpaRepository(null, new JpaProcedureExecutor(entityManager)));
        assertThrows(NullPointerException.class, () -> new CategoriaJustificacionAdminJpaRepository(entityManager, null));
    }

    @Test
    void consulta_la_vista_de_administracion() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{ID.toString(), "QA1", "Nombre", "desc", true, true}));
        assertEquals(List.of(new CategoriaJustificacionAdminProjection(ID, "QA1", "Nombre", "desc", true, true)), repository.consultarTodos());
        verify(entityManager).createNativeQuery(contains("FROM dbo."));
    }

    @Test
    void consulta_encapsula_fallos_tecnicos() {
        when(query.getResultList()).thenThrow(new PersistenceException("caida"));
        assertThrows(DatabaseOperationException.class, () -> repository.consultarTodos());
    }

    @Test
    void crea_con_binding_nombrado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(exito("CTG_007")));
        final ResultadoCatalogoAdminProjection r = repository.crear(new GuardarCategoriaJustificacionCommand(ID, "QA1", "Nombre", "desc", true, ACTOR));
        assertEquals(new ResultadoCatalogoAdminProjection(ID, "Listo."), r);
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_categoria_justificacion"));
        verify(query).setParameter("id", ID);
        verify(query).setParameter("codigo", "QA1");
        verify(query).setParameter("nombre", "Nombre");
        verify(query).setParameter("descripcion", "desc");
        verify(query).setParameter("requiereSoporte", true);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void actualiza_con_binding_nombrado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(exito("CTG_007")));
        repository.actualizar(new GuardarCategoriaJustificacionCommand(ID, "QA1", "Nombre", "desc", true, ACTOR));
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_categoria_justificacion"));
        verify(query).setParameter("id", ID);
        verify(query).setParameter("nombre", "Nombre");
        verify(query).setParameter("descripcion", "desc");
        verify(query).setParameter("requiereSoporte", true);
    }

    @Test
    void cambia_el_estado() {
        when(query.getResultList()).thenReturn(Collections.singletonList(exito("CTG_008")));
        assertEquals("Listo.", repository.cambiarEstado(ID, false, ACTOR).mensajeUsuario());
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_cambiar_estado_categoria_justificacion"));
        verify(query).setParameter("estaActiva", false);
    }

    @Test
    void traduce_los_codigos_de_la_db() {
        when(query.getResultList()).thenReturn(Collections.singletonList(rechazo("SEC_001")));
        assertThrows(ForbiddenException.class, () -> repository.cambiarEstado(ID, true, ACTOR));
        when(query.getResultList()).thenReturn(Collections.singletonList(rechazo("CTG_005")));
        assertThrows(ResourceNotFoundException.class, () -> repository.cambiarEstado(ID, true, ACTOR));
        when(query.getResultList()).thenReturn(Collections.singletonList(rechazo("CTG_003")));
        assertThrows(ConflictException.class, () -> repository.cambiarEstado(ID, false, ACTOR));
        when(query.getResultList()).thenReturn(Collections.singletonList(rechazo("CTG_006")));
        final GuardarCategoriaJustificacionCommand cmd = new GuardarCategoriaJustificacionCommand(ID, "QA1", "Nombre", "desc", true, ACTOR);
        assertThrows(ConflictException.class, () -> repository.crear(cmd));
        when(query.getResultList()).thenReturn(Collections.singletonList(rechazo("CTG_001")));
        assertThrows(ValidationException.class, () -> repository.actualizar(cmd));
        when(query.getResultList()).thenReturn(Collections.singletonList(rechazo("CTG_002")));
        assertThrows(ValidationException.class, () -> repository.crear(cmd));
        when(query.getResultList()).thenReturn(Collections.singletonList(rechazo("CTG_004")));
        assertThrows(ValidationException.class, () -> repository.crear(cmd));
    }
}
