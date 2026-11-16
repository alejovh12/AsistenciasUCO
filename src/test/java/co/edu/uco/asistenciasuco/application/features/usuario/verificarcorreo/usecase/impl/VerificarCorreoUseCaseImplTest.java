package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IDENT-001 BE-V01..BE-V04 (CA-06, CA-07, CA-11): el orden es validar el token, habilitar la cuenta en el IdP y recien
 * entonces consumir el token; si el IdP falla el token sigue vigente. Cualquier token invalido responde igual.
 * IDENT-002 ACT-V01..ACT-V07: una cuenta sin contrasena (registrada por otra persona) exige la que define su titular; se valida
 * con la politica, se fija como definitiva antes de habilitar la cuenta y ningun rechazo consume el token.
 */
class VerificarCorreoUseCaseImplTest {

    private static final String TOKEN = "A".repeat(43);
    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String ID_EXTERNO = "kc-user-1";
    private static final int DOCUMENTO = 1036100385;
    private static final String CLAVE_VALIDA = "Activa-2026x";

    private final List<String> orden = new ArrayList<>();

    private VerificacionCorreoRepositoryPort repositorio(final Optional<UUID> usuarioDelToken) {
        return new VerificacionCorreoRepositoryPort() {
            @Override
            public Optional<UUID> consultarUsuarioPorToken(final String token) {
                orden.add("consultar:" + token);
                return usuarioDelToken;
            }

            @Override
            public void confirmar(final String token) {
                orden.add("confirmar:" + token);
            }

            @Override
            public UUID consultarReenvio(final UUID grupoId, final UUID estudianteId, final UUID actorUsuarioId) {
                throw new UnsupportedOperationException("No usado por este test.");
            }

            @Override
            public UUID consultarReenvioActivacion(final UUID idUsuario, final UUID actorUsuarioId) {
                throw new UnsupportedOperationException("No usado por este test.");
            }
        };
    }

    /** Proveedor de identidad falso: {@code conContrasena} indica si la cuenta ya tiene credencial. */
    private IdentityProviderPort identidad(final boolean existeCuenta, final boolean falla, final boolean conContrasena,
                                           final boolean fallaCredenciales) {
        return new IdentityProviderPort() {
            @Override
            public CuentaIdentidadDTO crearCuenta(final CrearCuentaIdentidadDTO dto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void eliminarCuenta(final String idExterno) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void asignarRol(final String idExterno, final InstitutionalRole rol) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<CuentaIdentidadDTO> buscarCuentaPorIdUsuario(final UUID idUsuario) {
                orden.add("buscar:" + idUsuario);
                return existeCuenta ? Optional.of(new CuentaIdentidadDTO(ID_EXTERNO, false)) : Optional.empty();
            }

            @Override
            public boolean tieneContrasena(final String idExterno) {
                orden.add("credenciales:" + idExterno);
                if (fallaCredenciales) {
                    throw new IdentityProviderException("Keycloak no disponible.");
                }
                return conContrasena;
            }

            @Override
            public void establecerContrasena(final String idExterno, final String contrasena) {
                orden.add("clave:" + idExterno + ":" + contrasena);
                if (falla) {
                    throw new IdentityProviderException("Keycloak no disponible.");
                }
            }

            @Override
            public void confirmarCorreoCuenta(final String idExterno) {
                orden.add("idp:" + idExterno);
                if (falla) {
                    throw new IdentityProviderException("Keycloak no disponible.");
                }
            }
        };
    }

    private static MessageCatalogPort mensajes() {
        return new MessageCatalogPort() {
            @Override
            public String getUserMessage(final String code, final Object... args) {
                return "MSG:" + code;
            }

            @Override
            public String getTechnicalMessage(final String code, final Object... args) {
                return code;
            }

            @Override
            public Optional<String> findUserMessage(final String code) {
                return Optional.empty();
            }
        };
    }

    private static UsuarioRepositoryPort usuarios(final boolean existe) {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        when(usuarios.consultarUsuarioPorId(USUARIO)).thenReturn(existe
                ? Optional.of(new UsuarioIdentidadRepositoryProjection(USUARIO, UUID.randomUUID(), DOCUMENTO, "Ana", "Gomez",
                        "ana.gomez@uco.edu.co"))
                : Optional.empty());
        return usuarios;
    }

    private VerificarCorreoUseCaseImpl useCase(final Optional<UUID> usuario, final boolean existeCuenta, final boolean falla) {
        return new VerificarCorreoUseCaseImpl(repositorio(usuario), identidad(existeCuenta, falla, true, false),
                usuarios(true), mensajes());
    }

    private VerificarCorreoUseCaseImpl activacion(final boolean falla, final boolean fallaCredenciales, final boolean existeUsuario) {
        return new VerificarCorreoUseCaseImpl(repositorio(Optional.of(USUARIO)),
                identidad(true, falla, false, fallaCredenciales), usuarios(existeUsuario), mensajes());
    }

    @Test
    void BE_V01_token_valido_valida_habilita_en_el_idp_y_luego_consume_el_token() {
        final String mensaje = useCase(Optional.of(USUARIO), true, false).execute(TOKEN, null);

        assertEquals("MSG:VCE_006", mensaje);
        assertEquals(List.of("consultar:" + TOKEN, "buscar:" + USUARIO, "credenciales:" + ID_EXTERNO, "idp:" + ID_EXTERNO,
                "confirmar:" + TOKEN), orden);
    }

    @Test
    void BE_V01_con_cuenta_que_ya_tiene_contrasena_la_enviada_se_ignora() {
        final String mensaje = useCase(Optional.of(USUARIO), true, false).execute(TOKEN, CLAVE_VALIDA);

        assertEquals("MSG:VCE_006", mensaje);
        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("clave:")), orden.toString());
    }

    @Test
    void BE_V02_si_el_idp_falla_el_token_no_se_consume_y_el_error_es_controlado() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.of(USUARIO), true, true);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN, null));

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:")), orden.toString());
    }

    @Test
    void BE_V02_si_la_cuenta_no_existe_en_el_idp_el_token_no_se_consume() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.of(USUARIO), false, false);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN, null));

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:") || paso.startsWith("idp:")), orden.toString());
    }

    @Test
    void BE_V03_token_desconocido_responde_VCE_002_sin_tocar_el_idp() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.empty(), true, false);

        final ValidationException exception = assertThrows(ValidationException.class, () -> useCase.execute(TOKEN, null));

        assertEquals("VCE_002", exception.getCode());
        assertEquals("MSG:VCE_002", exception.getMessage());
        assertEquals(List.of("consultar:" + TOKEN), orden);
    }

    @Test
    void BE_V03_formato_invalido_responde_igual_que_un_token_desconocido_sin_consultar() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.of(USUARIO), true, false);

        for (final String invalido : new String[] {null, "", "   ", "corto", "A".repeat(44), "A".repeat(42) + "+"}) {
            final ValidationException exception = assertThrows(ValidationException.class, () -> useCase.execute(invalido, null));
            assertEquals("VCE_002", exception.getCode());
            assertEquals("MSG:VCE_002", exception.getMessage());
        }
        assertTrue(orden.isEmpty());
    }

    @Test
    void ACT_V01_cuenta_sin_contrasena_y_sin_clave_responde_VCE_009_sin_consumir_el_token() {
        final VerificarCorreoUseCaseImpl useCase = activacion(false, false, true);

        for (final String sinClave : new String[] {null, "", "   "}) {
            final ValidationException exception = assertThrows(ValidationException.class, () -> useCase.execute(TOKEN, sinClave));
            assertEquals("VCE_009", exception.getCode());
            assertEquals("MSG:VCE_009", exception.getMessage());
        }
        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:") || paso.startsWith("idp:")
                || paso.startsWith("clave:")), orden.toString());
    }

    @Test
    void ACT_V02_activacion_fija_la_clave_habilita_la_cuenta_y_consume_el_token_en_ese_orden() {
        final String mensaje = activacion(false, false, true).execute(TOKEN, CLAVE_VALIDA);

        assertEquals("MSG:VCE_006", mensaje);
        assertEquals(List.of("consultar:" + TOKEN, "buscar:" + USUARIO, "credenciales:" + ID_EXTERNO,
                "clave:" + ID_EXTERNO + ":" + CLAVE_VALIDA, "idp:" + ID_EXTERNO, "confirmar:" + TOKEN), orden);
    }

    @Test
    void ACT_V03_clave_fuera_de_politica_se_rechaza_sin_tocar_el_idp_ni_consumir_el_token() {
        final VerificarCorreoUseCaseImpl useCase = activacion(false, false, true);

        final ValidationException debil = assertThrows(ValidationException.class, () -> useCase.execute(TOKEN, "corta1A"));
        assertEquals("ERR_PASSWORD_POLITICA_INVALIDA", debil.getCode());
        final ValidationException igualDocumento = assertThrows(ValidationException.class,
                () -> useCase.execute(TOKEN, String.valueOf(DOCUMENTO)));
        assertEquals("ERR_PASSWORD_IGUAL_IDENTIFICACION", igualDocumento.getCode());

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:") || paso.startsWith("idp:")
                || paso.startsWith("clave:")), orden.toString());
    }

    @Test
    void ACT_V04_si_el_idp_falla_al_fijar_la_clave_el_token_sigue_vigente() {
        final VerificarCorreoUseCaseImpl useCase = activacion(true, false, true);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN, CLAVE_VALIDA));

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:") || paso.startsWith("idp:")), orden.toString());
    }

    @Test
    void ACT_V05_si_no_se_pueden_consultar_las_credenciales_el_token_sigue_vigente() {
        final VerificarCorreoUseCaseImpl useCase = activacion(false, true, true);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN, CLAVE_VALIDA));

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:") || paso.startsWith("clave:")), orden.toString());
    }

    @Test
    void ACT_V06_si_no_existe_el_usuario_institucional_no_se_fija_clave_ni_se_consume_el_token() {
        final VerificarCorreoUseCaseImpl useCase = activacion(false, false, false);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN, CLAVE_VALIDA));

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:") || paso.startsWith("clave:")), orden.toString());
    }

    @Test
    void los_colaboradores_son_obligatorios() {
        final IdentityProviderPort idPort = identidad(true, false, true, false);
        final MessageCatalogPort msgPort = mensajes();
        final VerificacionCorreoRepositoryPort repoPort = repositorio(Optional.empty());
        final UsuarioRepositoryPort usuarioPort = usuarios(true);
        assertThrows(NullPointerException.class, () -> new VerificarCorreoUseCaseImpl(null, idPort, usuarioPort, msgPort));
        assertThrows(NullPointerException.class, () -> new VerificarCorreoUseCaseImpl(repoPort, null, usuarioPort, msgPort));
        assertThrows(NullPointerException.class, () -> new VerificarCorreoUseCaseImpl(repoPort, idPort, null, msgPort));
        assertThrows(NullPointerException.class, () -> new VerificarCorreoUseCaseImpl(repoPort, idPort, usuarioPort, null));
    }
}
