package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 BE-V01..BE-V04 (CA-06, CA-07, CA-11): el orden es validar el token, habilitar la cuenta en el IdP y recien
 * entonces consumir el token; si el IdP falla el token sigue vigente. Cualquier token invalido responde igual.
 */
class VerificarCorreoUseCaseImplTest {

    private static final String TOKEN = "A".repeat(43);
    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String ID_EXTERNO = "kc-user-1";

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
        };
    }

    private IdentityProviderPort identidad(final boolean existeCuenta, final boolean falla) {
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

    private VerificarCorreoUseCaseImpl useCase(final Optional<UUID> usuario, final boolean existeCuenta, final boolean falla) {
        return new VerificarCorreoUseCaseImpl(repositorio(usuario), identidad(existeCuenta, falla), mensajes());
    }

    @Test
    void BE_V01_token_valido_valida_habilita_en_el_idp_y_luego_consume_el_token() {
        final String mensaje = useCase(Optional.of(USUARIO), true, false).execute(TOKEN);

        assertEquals("MSG:VCE_006", mensaje);
        assertEquals(List.of("consultar:" + TOKEN, "buscar:" + USUARIO, "idp:" + ID_EXTERNO, "confirmar:" + TOKEN), orden);
    }

    @Test
    void BE_V02_si_el_idp_falla_el_token_no_se_consume_y_el_error_es_controlado() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.of(USUARIO), true, true);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN));

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:")), orden.toString());
    }

    @Test
    void BE_V02_si_la_cuenta_no_existe_en_el_idp_el_token_no_se_consume() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.of(USUARIO), false, false);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN));

        assertTrue(orden.stream().noneMatch(paso -> paso.startsWith("confirmar:") || paso.startsWith("idp:")), orden.toString());
    }

    @Test
    void BE_V03_token_desconocido_responde_VCE_002_sin_tocar_el_idp() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.empty(), true, false);

        final ValidationException exception = assertThrows(ValidationException.class, () -> useCase.execute(TOKEN));

        assertEquals("VCE_002", exception.getCode());
        assertEquals("MSG:VCE_002", exception.getMessage());
        assertEquals(List.of("consultar:" + TOKEN), orden);
    }

    @Test
    void BE_V03_formato_invalido_responde_igual_que_un_token_desconocido_sin_consultar() {
        final VerificarCorreoUseCaseImpl useCase = useCase(Optional.of(USUARIO), true, false);

        for (final String invalido : new String[] {null, "", "   ", "corto", "A".repeat(44), "A".repeat(42) + "+"}) {
            final ValidationException exception = assertThrows(ValidationException.class, () -> useCase.execute(invalido));
            assertEquals("VCE_002", exception.getCode());
            assertEquals("MSG:VCE_002", exception.getMessage());
        }
        assertTrue(orden.isEmpty());
    }

    @Test
    void los_colaboradores_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new VerificarCorreoUseCaseImpl(null, identidad(true, false), mensajes()));
        assertThrows(NullPointerException.class,
                () -> new VerificarCorreoUseCaseImpl(repositorio(Optional.empty()), null, mensajes()));
        assertThrows(NullPointerException.class,
                () -> new VerificarCorreoUseCaseImpl(repositorio(Optional.empty()), identidad(true, false), null));
    }
}
