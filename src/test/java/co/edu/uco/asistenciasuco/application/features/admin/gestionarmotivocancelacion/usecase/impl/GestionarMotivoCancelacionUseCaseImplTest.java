package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.domain.MotivoCancelacionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.MotivoCancelacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.GuardarMotivoCancelacionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.interactor.GestionarMotivoCancelacionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MotivoCancelacionAdminPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarMotivoCancelacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionAdminProjection;
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

/** Migracion 65: casos de uso e interactor de la administracion del catalogo (MotivoCancelacion). */
class GestionarMotivoCancelacionUseCaseImplTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final MotivoCancelacionAdminPort port = mock(MotivoCancelacionAdminPort.class);
    private final GestionarMotivoCancelacionUseCaseImpl useCase = new GestionarMotivoCancelacionUseCaseImpl(port);
    private final GestionarMotivoCancelacionInteractor interactor = new GestionarMotivoCancelacionInteractor(useCase);

    @Test
    void constructores_rechazan_nulos() {
        assertThrows(NullPointerException.class, () -> new GestionarMotivoCancelacionUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new GestionarMotivoCancelacionInteractor(null));
    }

    @Test
    void consultar_devuelve_todos_los_registros() {
        when(port.consultarTodos()).thenReturn(List.of(new MotivoCancelacionAdminProjection(ID, "QA_1", "Nombre", 4, true)));
        assertEquals(List.of(new MotivoCancelacionAdminDTO(ID, "QA_1", "Nombre", 4, true)), interactor.consultar());
    }

    @Test
    void crear_recorta_genera_id_cuando_falta_y_devuelve_el_mensaje() {
        when(port.crear(new GuardarMotivoCancelacionCommand(ID, "QA_1", "Nombre", 4, ACTOR))).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        final ResultadoCatalogoAdminDTO r = interactor.crear(new GuardarMotivoCancelacionDTO(ID, "QA_1", " Nombre ", 4, ACTOR));
        assertEquals(new ResultadoCatalogoAdminDTO(ID, "ok"), r);
    }

    @Test
    void crear_sin_id_genera_uno_nuevo() {
        when(port.crear(org.mockito.ArgumentMatchers.any())).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        final var dom = new MotivoCancelacionDomain(null, "QA_1", " Nombre ", 4, ACTOR);
        useCase.crear(dom);
        final var captor = org.mockito.ArgumentCaptor.forClass(GuardarMotivoCancelacionCommand.class);
        verify(port).crear(captor.capture());
        assertEquals(false, captor.getValue().id() == null);
    }

    @Test
    void crear_con_descripcion_o_orden_vacio() {
        when(port.crear(new GuardarMotivoCancelacionCommand(ID, "QA_1", "Nombre", null, ACTOR))).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        assertEquals("ok", interactor.crear(new GuardarMotivoCancelacionDTO(ID, "QA_1", "Nombre", null, ACTOR)).mensajeUsuario());
    }

    @Test
    void crear_exige_codigo_nombre_y_usuario() {
        final MotivoCancelacionDomain sinCodigo = new MotivoCancelacionDomain(ID, null, "Nombre", 4, ACTOR);
        final MotivoCancelacionDomain sinNombre = new MotivoCancelacionDomain(ID, "QA_1", null, 4, ACTOR);
        assertThrows(CrosscuttingException.class, () -> useCase.crear(sinCodigo));
        assertThrows(CrosscuttingException.class, () -> useCase.crear(sinNombre));
        assertThrows(CrosscuttingException.class, () -> useCase.crear(null));
        verifyNoInteractions(port);
    }

    @Test
    void actualizar_no_envia_codigo() {
        when(port.actualizar(new GuardarMotivoCancelacionCommand(ID, null, "Nombre", 4, ACTOR))).thenReturn(new ResultadoCatalogoAdminProjection(ID, "ok"));
        assertEquals("ok", interactor.actualizar(new GuardarMotivoCancelacionDTO(ID, "QA_1", " Nombre ", 4, ACTOR)).mensajeUsuario());
    }

    @Test
    void actualizar_exige_id() {
        final MotivoCancelacionDomain sinId = new MotivoCancelacionDomain(null, null, "Nombre", 4, ACTOR);
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
