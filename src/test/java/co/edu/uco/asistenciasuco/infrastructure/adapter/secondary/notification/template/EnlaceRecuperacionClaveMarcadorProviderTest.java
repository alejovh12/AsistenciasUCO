package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU177: el emisor de enlaces de un solo uso arma tambien el enlace para definir una contrasena nueva, con su propia
 * ruta y su propio registro del hash, sin alterar el enlace de verificacion de correo.
 */
class EnlaceRecuperacionClaveMarcadorProviderTest {

    private static final String PLATFORM_URL = "https://asistencia.uco.edu.co/";
    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String RECHAZO = "El usuario no esta disponible para definir una contrasena nueva.";

    private final List<byte[]> hashesVerificacion = new ArrayList<>();
    private final List<byte[]> hashesRecuperacion = new ArrayList<>();

    private VerificacionCorreoMarcadorProvider proveedor(final TokenVerificacionCorreoRepository recuperacion) {
        return new VerificacionCorreoMarcadorProvider(
                (idUsuario, hash) -> hashesVerificacion.add(hash), PLATFORM_URL, new SecureRandom())
                .conEnlace(Notificacion.TIPO_RECUPERACION_CLAVE, VerificacionCorreoMarcadorProvider.RUTA_RECUPERACION_CLAVE,
                        recuperacion, RECHAZO);
    }

    @Test
    void el_aviso_de_recuperacion_lleva_el_enlace_a_la_pagina_de_clave_nueva_y_registra_solo_el_hash() throws Exception {
        final Map<String, String> marcadores = proveedor((idUsuario, hash) -> {
            assertEquals(USUARIO, idUsuario);
            hashesRecuperacion.add(hash);
        }).resolver(Notificacion.TIPO_RECUPERACION_CLAVE, List.of(USUARIO));

        final String prefijo = "https://asistencia.uco.edu.co/restablecer-clave#token=";
        final String enlace = marcadores.get("enlace");
        assertTrue(enlace.startsWith(prefijo), enlace);
        final String token = enlace.substring(prefijo.length());
        assertTrue(TokenHashHelper.esFormatoValido(token));
        assertEquals(1, hashesRecuperacion.size());
        assertArrayEquals(TokenHashHelper.sha256(token), hashesRecuperacion.getFirst());
        assertFalse(Arrays.toString(hashesRecuperacion.getFirst()).contains(token));
        assertTrue(hashesVerificacion.isEmpty(), "No debe tocar el token de verificacion de correo.");
    }

    @Test
    void el_enlace_de_verificacion_de_correo_no_cambia_al_agregar_el_de_recuperacion() throws Exception {
        final Map<String, String> marcadores = proveedor((idUsuario, hash) -> hashesRecuperacion.add(hash))
                .resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO));

        assertTrue(marcadores.get("enlace").startsWith("https://asistencia.uco.edu.co/verificar-correo#token="));
        assertEquals(1, hashesVerificacion.size());
        assertTrue(hashesRecuperacion.isEmpty());
    }

    @Test
    void otros_tipos_de_aviso_no_reciben_marcadores_ni_emiten_tokens() throws Exception {
        final VerificacionCorreoMarcadorProvider proveedor = proveedor((idUsuario, hash) -> hashesRecuperacion.add(hash));

        assertTrue(proveedor.resolver(Notificacion.TIPO_SESION_CANCELADA, List.of(USUARIO)).isEmpty());
        assertTrue(proveedor.resolver(null, List.of(USUARIO)).isEmpty());
        assertTrue(hashesRecuperacion.isEmpty());
    }

    @Test
    void un_usuario_no_disponible_es_un_fallo_permanente_con_el_motivo_de_recuperacion() {
        final VerificacionCorreoMarcadorProvider proveedor = proveedor((idUsuario, hash) -> {
            throw new ResourceNotFoundException("VCE_007", "El usuario no existe o no está activo.");
        });

        final NotificacionNoEntregableException error = assertThrows(NotificacionNoEntregableException.class,
                () -> proveedor.resolver(Notificacion.TIPO_RECUPERACION_CLAVE, List.of(USUARIO)));
        assertEquals(RECHAZO, error.getMessage());
    }

    @Test
    void exige_un_solo_destinatario() {
        final VerificacionCorreoMarcadorProvider proveedor = proveedor((idUsuario, hash) -> hashesRecuperacion.add(hash));

        assertThrows(NotificacionNoEntregableException.class,
                () -> proveedor.resolver(Notificacion.TIPO_RECUPERACION_CLAVE, List.of(USUARIO, UUID.randomUUID())));
        assertThrows(NotificacionNoEntregableException.class,
                () -> proveedor.resolver(Notificacion.TIPO_RECUPERACION_CLAVE, List.of()));
        assertTrue(hashesRecuperacion.isEmpty());
    }

    @Test
    void los_datos_del_enlace_son_obligatorios() {
        final VerificacionCorreoMarcadorProvider base = new VerificacionCorreoMarcadorProvider(
                (idUsuario, hash) -> { }, PLATFORM_URL, new SecureRandom());
        final TokenVerificacionCorreoRepository repo = (idUsuario, hash) -> { };

        assertThrows(NullPointerException.class, () -> base.conEnlace(null, "/x#token=", repo, RECHAZO));
        assertThrows(NullPointerException.class, () -> base.conEnlace("TIPO", null, repo, RECHAZO));
        assertThrows(NullPointerException.class, () -> base.conEnlace("TIPO", "/x#token=", null, RECHAZO));
        assertThrows(NullPointerException.class, () -> base.conEnlace("TIPO", "/x#token=", repo, null));
    }
}
