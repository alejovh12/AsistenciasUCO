package co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** IDENT-002: reenvio del enlace de activacion por el responsable del alta (alcance, reglas de BD y aviso). */
class ReenviarActivacionCuentaServiceTest {

    private static final UUID ACTOR = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DESTINO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String CORREO = "jorge.henao@uco.edu.co";

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final VerificacionCorreoRepositoryPort verificaciones = mock(VerificacionCorreoRepositoryPort.class);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final MessageCatalogPort mensajes = mock(MessageCatalogPort.class);
    private final ReenviarActivacionCuentaService servicio =
            new ReenviarActivacionCuentaService(scope, verificaciones, usuarios, notificaciones, mensajes);

    @BeforeEach
    void setUp() {
        when(mensajes.getUserMessage("ACT_001")).thenReturn("Se envio de nuevo el enlace de activacion.");
    }

    private void destinoEnAlcance() {
        when(scope.findUsuarioIdByEmail(CORREO)).thenReturn(Optional.of(DESTINO));
        when(scope.canGestionarCuenta(ACTOR, DESTINO)).thenReturn(true);
        when(verificaciones.consultarReenvioActivacion(DESTINO, ACTOR)).thenReturn(DESTINO);
    }

    private Notificacion avisoEnviado() {
        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        return aviso.getValue();
    }

    @Test
    void reenvia_con_el_correo_normalizado_y_el_rol_de_mayor_responsabilidad() {
        destinoEnAlcance();
        when(usuarios.consultarUsuarioPorId(DESTINO)).thenReturn(Optional.of(
                new UsuarioIdentidadRepositoryProjection(DESTINO, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", CORREO)));
        when(scope.findRolesActivosByUsuario(DESTINO)).thenReturn(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR));

        assertEquals("Se envio de nuevo el enlace de activacion.", servicio.reenviar(ACTOR, "  Jorge.Henao@UCO.edu.co "));

        final Notificacion aviso = avisoEnviado();
        assertEquals(Notificacion.TIPO_ACTIVACION_CUENTA, aviso.tipo());
        assertEquals(DESTINO, aviso.destinatarios().getFirst().idUsuario());
        assertEquals("COORDINADOR", aviso.destinatarios().getFirst().rol());
        assertEquals("coordinador(a) de programa", aviso.datos().get("rol"));
        assertEquals("Jorge", aviso.datos().get("nombre"));
    }

    @Test
    void sin_roles_activos_ni_datos_del_usuario_el_aviso_usa_estudiante_y_nombre_vacio() {
        destinoEnAlcance();
        when(usuarios.consultarUsuarioPorId(DESTINO)).thenReturn(Optional.empty());
        when(scope.findRolesActivosByUsuario(DESTINO)).thenReturn(Set.of());

        servicio.reenviar(ACTOR, CORREO);

        final Notificacion aviso = avisoEnviado();
        assertEquals("ESTUDIANTE", aviso.destinatarios().getFirst().rol());
        assertEquals("", aviso.datos().get("nombre"));
    }

    @Test
    void correo_vacio_o_desconocido_es_404_sin_tocar_la_base() {
        assertEquals(MensajeUsuarioErrorCode.NOF_APP_010, assertThrows(ResourceNotFoundException.class, () -> servicio.reenviar(ACTOR, null)).getErrorDefinition().orElseThrow());
        assertEquals(MensajeUsuarioErrorCode.NOF_APP_010, assertThrows(ResourceNotFoundException.class, () -> servicio.reenviar(ACTOR, "  ")).getErrorDefinition().orElseThrow());
        when(scope.findUsuarioIdByEmail(CORREO)).thenReturn(Optional.empty());
        assertEquals(MensajeUsuarioErrorCode.NOF_APP_010, assertThrows(ResourceNotFoundException.class, () -> servicio.reenviar(ACTOR, CORREO)).getErrorDefinition().orElseThrow());

        verifyNoInteractions(verificaciones, notificaciones);
    }

    @Test
    void cuenta_fuera_del_alcance_del_actor_es_403() {
        when(scope.findUsuarioIdByEmail(CORREO)).thenReturn(Optional.of(DESTINO));
        when(scope.canGestionarCuenta(ACTOR, DESTINO)).thenReturn(false);

        assertEquals(MensajeUsuarioErrorCode.ACC_APP_013, assertThrows(ForbiddenException.class, () -> servicio.reenviar(ACTOR, CORREO)).getErrorDefinition().orElseThrow());
        verifyNoInteractions(verificaciones, notificaciones);
    }

    @Test
    void cuenta_ya_activa_propaga_el_409_de_la_base_sin_avisar() {
        when(scope.findUsuarioIdByEmail(CORREO)).thenReturn(Optional.of(DESTINO));
        when(scope.canGestionarCuenta(ACTOR, DESTINO)).thenReturn(true);
        final ConflictException activa = new ConflictException("ACT_002", "La cuenta ya esta activa.");
        when(verificaciones.consultarReenvioActivacion(DESTINO, ACTOR)).thenThrow(activa);

        assertSame(activa, assertThrows(ConflictException.class, () -> servicio.reenviar(ACTOR, CORREO)));
        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void actor_y_colaboradores_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> servicio.reenviar(null, CORREO));
        assertThrows(NullPointerException.class, () -> new ReenviarActivacionCuentaService(null, verificaciones, usuarios, notificaciones, mensajes));
        assertThrows(NullPointerException.class, () -> new ReenviarActivacionCuentaService(scope, null, usuarios, notificaciones, mensajes));
        assertThrows(NullPointerException.class, () -> new ReenviarActivacionCuentaService(scope, verificaciones, null, notificaciones, mensajes));
        assertThrows(NullPointerException.class, () -> new ReenviarActivacionCuentaService(scope, verificaciones, usuarios, null, mensajes));
        assertThrows(NullPointerException.class, () -> new ReenviarActivacionCuentaService(scope, verificaciones, usuarios, notificaciones, null));
    }
}
