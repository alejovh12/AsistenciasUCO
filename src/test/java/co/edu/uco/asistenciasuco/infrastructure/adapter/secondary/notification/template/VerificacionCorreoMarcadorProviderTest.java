package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 BE-M01..BE-M04: el token se genera al entregar, solo su SHA-256 llega a la base de datos y el enlace usa el
 * fragmento {@code #token=}. Un correo ya verificado es un fallo permanente; cualquier otro fallo es reintentable.
 */
class VerificacionCorreoMarcadorProviderTest {

    private static final String PLATFORM_URL = "https://asistencia.uco.edu.co";
    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    private final List<UUID> usuarios = new ArrayList<>();
    private final List<byte[]> hashes = new ArrayList<>();

    private TokenVerificacionCorreoRepository repositorioOk() {
        return (idUsuario, hash) -> {
            usuarios.add(idUsuario);
            hashes.add(hash);
        };
    }

    private VerificacionCorreoMarcadorProvider provider(final TokenVerificacionCorreoRepository repositorio, final String url) {
        return new VerificacionCorreoMarcadorProvider(repositorio, url, new SecureRandom());
    }

    @Test
    void BE_M01_genera_un_token_de_43_caracteres_y_guarda_solo_su_hash() throws Exception {
        final Map<String, String> marcadores =
                provider(repositorioOk(), PLATFORM_URL).resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO));

        final String enlace = marcadores.get("enlace");
        final String prefijo = PLATFORM_URL + "/verificar-correo#token=";
        assertTrue(enlace.startsWith(prefijo), enlace);
        final String token = enlace.substring(prefijo.length());
        assertEquals(43, token.length());
        assertTrue(TokenHashHelper.esFormatoValido(token));
        assertEquals(List.of(USUARIO), usuarios);
        assertEquals(1, hashes.size());
        assertArrayEquals(TokenHashHelper.sha256(token), hashes.getFirst());
        assertFalse(Arrays.toString(hashes.getFirst()).contains(token));
    }

    @Test
    void BE_M01_la_url_con_barra_final_no_duplica_la_barra() throws Exception {
        final Map<String, String> marcadores = provider(repositorioOk(), PLATFORM_URL + "/")
                .resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO));

        assertTrue(marcadores.get("enlace").startsWith(PLATFORM_URL + "/verificar-correo#token="));
    }

    @Test
    void BE_M01_cada_llamada_emite_un_token_distinto() throws Exception {
        final VerificacionCorreoMarcadorProvider provider = provider(repositorioOk(), PLATFORM_URL);

        final String primero = provider.resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO)).get("enlace");
        final String segundo = provider.resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO)).get("enlace");

        assertNotEquals(primero, segundo);
    }

    @Test
    void otros_tipos_no_emiten_token_ni_marcadores() throws Exception {
        final Map<String, String> marcadores =
                provider(repositorioOk(), PLATFORM_URL).resolver(Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, List.of(USUARIO));

        assertTrue(marcadores.isEmpty());
        assertTrue(hashes.isEmpty());
    }

    @Test
    void exige_exactamente_un_destinatario() throws Exception {
        final VerificacionCorreoMarcadorProvider provider = provider(repositorioOk(), PLATFORM_URL);

        assertThrows(NotificacionNoEntregableException.class,
                () -> provider.resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of()));
        assertThrows(NotificacionNoEntregableException.class,
                () -> provider.resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO, UUID.randomUUID())));
        assertTrue(hashes.isEmpty());
    }

    @Test
    void sin_url_de_plataforma_configurada_es_un_fallo_permanente_y_no_emite_token() throws Exception {
        assertThrows(NotificacionNoEntregableException.class, () -> provider(repositorioOk(), "  ")
                .resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO)));
        assertThrows(NotificacionNoEntregableException.class, () -> provider(repositorioOk(), null)
                .resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO)));
        assertTrue(hashes.isEmpty());
    }

    @Test
    void BE_M02_un_correo_ya_verificado_es_un_fallo_permanente() throws Exception {
        final TokenVerificacionCorreoRepository yaVerificado = (idUsuario, hash) -> {
            throw new ConflictException("VCE_005", "El correo de este estudiante ya fue verificado.");
        };

        assertThrows(NotificacionNoEntregableException.class, () -> provider(yaVerificado, PLATFORM_URL)
                .resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO)));
    }

    @Test
    void BE_M02_un_usuario_inexistente_o_inactivo_es_un_fallo_permanente() throws Exception {
        final TokenVerificacionCorreoRepository inexistente = (idUsuario, hash) -> {
            throw new ResourceNotFoundException("El usuario no existe o no esta activo.");
        };

        assertThrows(NotificacionNoEntregableException.class, () -> provider(inexistente, PLATFORM_URL)
                .resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO)));
    }

    @Test
    void BE_M03_un_fallo_tecnico_de_la_base_de_datos_se_propaga_para_reintentar() throws Exception {
        final TokenVerificacionCorreoRepository caido = (idUsuario, hash) -> {
            throw new IllegalStateException("SQL Server no disponible.");
        };

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> provider(caido, PLATFORM_URL)
                .resolver(Notificacion.TIPO_VERIFICACION_CORREO, List.of(USUARIO)));
        assertEquals("SQL Server no disponible.", exception.getMessage());
    }

    @Test
    void los_colaboradores_son_obligatorios() throws Exception {
        assertThrows(NullPointerException.class,
                () -> new VerificacionCorreoMarcadorProvider(null, PLATFORM_URL, new SecureRandom()));
        assertThrows(NullPointerException.class,
                () -> new VerificacionCorreoMarcadorProvider(repositorioOk(), PLATFORM_URL, null));
    }
}
