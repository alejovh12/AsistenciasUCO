package co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.SecureRandom;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IDENT-002: secreto legado inutilizable para la columna {@code Usuario.password} y aviso {@code ACTIVACION_CUENTA} de
 * mejor esfuerzo para las cuentas registradas por otra persona.
 */
class ActivacionCuentaServiceTest {

    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final PasswordEncoderPort codificador = mock(PasswordEncoderPort.class);
    private final ActivacionCuentaService servicio = new ActivacionCuentaService(notificaciones, codificador, new SecureRandom());

    @Test
    void la_credencial_legada_es_el_hash_de_un_secreto_aleatorio_distinto_en_cada_alta() {
        when(codificador.encode(anyString())).thenAnswer(invocacion -> "{hash}" + invocacion.getArgument(0));

        final String primera = servicio.credencialLegadaInutilizable();
        final String segunda = servicio.credencialLegadaInutilizable();

        assertTrue(primera.startsWith("{hash}"));
        // 32 bytes en Base64 URL sin relleno son 43 caracteres.
        assertEquals("{hash}".length() + 43, primera.length());
        assertNotEquals(primera, segunda);
    }

    @Test
    void el_aviso_va_al_usuario_con_nombre_propio_y_la_descripcion_del_rol_sin_correo_ni_token() {
        servicio.avisar(USUARIO, "JORGE", InstitutionalRole.DOCENTE);

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        final Notificacion notificacion = aviso.getValue();
        assertEquals(Notificacion.TIPO_ACTIVACION_CUENTA, notificacion.tipo());
        assertEquals(ActivacionCuentaService.ASUNTO, notificacion.asunto());
        assertEquals(1, notificacion.destinatarios().size());
        assertEquals("DOCENTE", notificacion.destinatarios().getFirst().rol());
        assertEquals(USUARIO, notificacion.destinatarios().getFirst().idUsuario());
        assertNull(notificacion.destinatarios().getFirst().correo());
        assertEquals("Jorge", notificacion.datos().get("nombre"));
        assertEquals("docente", notificacion.datos().get("rol"));
        assertFalse(notificacion.toString().toLowerCase(java.util.Locale.ROOT).contains("token"));
    }

    @Test
    void sin_primer_nombre_el_marcador_queda_vacio() {
        assertEquals("", ActivacionCuentaService.notificacion(USUARIO, null, InstitutionalRole.ESTUDIANTE).datos().get("nombre"));
    }

    @Test
    void cada_rol_tiene_su_descripcion_en_el_aviso() {
        assertEquals("administrador(a) institucional", ActivacionCuentaService.descripcionRol(InstitutionalRole.ADMINISTRADOR));
        assertEquals("decano(a) de facultad", ActivacionCuentaService.descripcionRol(InstitutionalRole.DECANO));
        assertEquals("coordinador(a) de programa", ActivacionCuentaService.descripcionRol(InstitutionalRole.COORDINADOR));
        assertEquals("docente", ActivacionCuentaService.descripcionRol(InstitutionalRole.DOCENTE));
        assertEquals("estudiante", ActivacionCuentaService.descripcionRol(InstitutionalRole.ESTUDIANTE));
    }

    @Test
    void un_fallo_del_outbox_no_se_propaga() {
        doThrow(new IllegalStateException("cola caida")).when(notificaciones).notificar(any());

        assertDoesNotThrow(() -> servicio.avisar(USUARIO, "Ana", InstitutionalRole.ESTUDIANTE));
    }

    @Test
    void usuario_y_rol_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> servicio.avisar(null, "Ana", InstitutionalRole.ESTUDIANTE));
        assertThrows(NullPointerException.class, () -> servicio.avisar(USUARIO, "Ana", null));
    }

    @Test
    void los_colaboradores_son_obligatorios() {
        final var secureRandom2 = new SecureRandom();
        assertThrows(NullPointerException.class, () -> new ActivacionCuentaService(null, codificador, secureRandom2));
        final var secureRandom = new SecureRandom();
        assertThrows(NullPointerException.class, () -> new ActivacionCuentaService(notificaciones, null, secureRandom));
        assertThrows(NullPointerException.class, () -> new ActivacionCuentaService(notificaciones, codificador, null));
    }
}
