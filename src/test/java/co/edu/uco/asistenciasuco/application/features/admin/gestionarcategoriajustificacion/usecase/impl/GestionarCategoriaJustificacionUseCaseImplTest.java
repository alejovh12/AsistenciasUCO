package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.usecase.domain.CategoriaJustificacionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.CategoriaJustificacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.GuardarCategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.interactor.GestionarCategoriaJustificacionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CategoriaJustificacionAdminPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarCategoriaJustificacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Migracion 65: casos de uso e interactor de la administracion del catalogo (CategoriaJustificacion). */
class GestionarCategoriaJustificacionUseCaseImplTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final CategoriaJustificacionAdminPort port = mock(CategoriaJustificacionAdminPort.class);
    private final GestionarCategoriaJustificacionUseCaseImpl useCase = new GestionarCategoriaJustificacionUseCaseImpl(port);
    private final GestionarCategoriaJustificacionInteractor interactor = new GestionarCategoriaJustificacionInteractor(useCase);

    @Test
    void constructores_rechazan_nulos() {
        assertThrows(NullPointerException.class, () -> new GestionarCategoriaJustificacionUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new GestionarCategoriaJustificacionInteractor(null));
    }

    @Test
    void consultar_devuelve_todos_los_registros() {
        when(port.consultarTodos()).thenReturn(List.of(new CategoriaJustificacionAdminProjection(ID, "QA1", "Nombre", "desc", true, true)));
        assertEquals(List.of(new CategoriaJustificacionAdminDTO(ID, "QA1", "Nombre", "desc", true, true)), interactor.consultar());
    }

    @Test
    void crear_recorta_genera_id_cuando_falta_y_devuelve_el_mensaje() {
        when(port.crear(new GuardarCategoriaJustificacionCommand(ID, "QA1", "Nombre", "desc", true, ACTOR))).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        final ResultadoCatalogoAdminDTO r = interactor.crear(new GuardarCategoriaJustificacionDTO(ID, "QA1", " Nombre ", "  desc  ", true, ACTOR));
        assertEquals(new ResultadoCatalogoAdminDTO(ID, "ok"), r);
    }

    @Test
    void crear_sin_id_genera_uno_nuevo() {
        when(port.crear(org.mockito.ArgumentMatchers.any())).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        final var dom = new CategoriaJustificacionDomain(null, "QA1", " Nombre ", "  desc  ", true, ACTOR);
        useCase.crear(dom);
        final var captor = org.mockito.ArgumentCaptor.forClass(GuardarCategoriaJustificacionCommand.class);
        verify(port).crear(captor.capture());
        assertEquals(false, captor.getValue().id() == null);
    }

    @Test
    void crear_con_descripcion_o_orden_vacio() {
        when(port.crear(new GuardarCategoriaJustificacionCommand(ID, "QA1", "Nombre", null, true, ACTOR))).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        assertEquals("ok", interactor.crear(new GuardarCategoriaJustificacionDTO(ID, "QA1", "Nombre", "  ", true, ACTOR)).mensajeUsuario());
    }

    @Test
    void crear_exige_codigo_nombre_y_usuario() {
        final CategoriaJustificacionDomain sinCodigo = new CategoriaJustificacionDomain(ID, null, "Nombre", "d", true, ACTOR);
        final CategoriaJustificacionDomain sinNombre = new CategoriaJustificacionDomain(ID, "QA1", null, "d", true, ACTOR);
        assertThrows(CrosscuttingException.class, () -> useCase.crear(sinCodigo));
        assertThrows(CrosscuttingException.class, () -> useCase.crear(sinNombre));
        assertThrows(CrosscuttingException.class, () -> useCase.crear(null));
        verifyNoInteractions(port);
    }

    @Test
    void requiere_soporte_es_obligatorio() {
        final CategoriaJustificacionDomain dom = new CategoriaJustificacionDomain(ID, "QA1", "Nombre", "d", null, ACTOR);
        assertThrows(CrosscuttingException.class, () -> useCase.crear(dom));
    }

    @Test
    void actualizar_no_envia_codigo() {
        when(port.actualizar(new GuardarCategoriaJustificacionCommand(ID, null, "Nombre", "desc", true, ACTOR))).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        assertEquals("ok", interactor.actualizar(new GuardarCategoriaJustificacionDTO(ID, "QA1", " Nombre ", "  desc  ", true, ACTOR)).mensajeUsuario());
    }

    @Test
    void actualizar_exige_id() {
        final CategoriaJustificacionDomain sinId = new CategoriaJustificacionDomain(null, null, "Nombre", "d", true, ACTOR);
        assertThrows(CrosscuttingException.class, () -> useCase.actualizar(sinId));
    }

    @Test
    void cambiar_estado_delega_y_valida() {
        when(port.cambiarEstado(ID, false, ACTOR)).thenReturn(new ResultadoCatalogoAdminProjection(ID, "inactivo"));
        assertEquals("inactivo", interactor.cambiarEstado(ID, false, ACTOR).mensajeUsuario());
        assertThrows(CrosscuttingException.class, () -> useCase.cambiarEstado(null, true, ACTOR));
        assertThrows(CrosscuttingException.class, () -> useCase.cambiarEstado(ID, null, ACTOR));
        assertThrows(CrosscuttingException.class, () -> useCase.cambiarEstado(ID, true, null));
    }
}
