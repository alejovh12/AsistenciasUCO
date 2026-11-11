package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU177: el orden es consultar el enlace, validar la contrasena, fijarla en el proveedor de identidad y solo entonces
 * consumir el enlace; si el proveedor falla el enlace sigue vigente. Cualquier enlace que no sirve responde igual.
 */
class RestablecerClaveUseCaseImplTest {

    private static final String TOKEN = "A".repeat(43);
    private static final String CLAVE = "ClaveNueva-2026";
    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String ID_EXTERNO = "kc-user-1";
    private static final String MENSAJE_INVALIDO = "El enlace para definir la contraseña no es válido o ya venció. Solicite uno nuevo.";
    private static final String MENSAJE_EXITO = "Su contraseña fue actualizada. Ya puede iniciar sesión.";

    private final RecuperacionClaveRepositoryPort repositorio = mock(RecuperacionClaveRepositoryPort.class);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final IdentityProviderPort identidad = mock(IdentityProviderPort.class);
    private final ExternalIdentityLinkPort vinculos = mock(ExternalIdentityLinkPort.class);
    private final MessageCatalogPort mensajes = mock(MessageCatalogPort.class);
    private final RestablecerClaveUseCaseImpl useCase =
            new RestablecerClaveUseCaseImpl(repositorio, usuarios, identidad, vinculos, mensajes);

    @BeforeEach
    void setUp() {
        when(mensajes.getUserMessage("RCL_002")).thenReturn(MENSAJE_INVALIDO);
        when(mensajes.getUserMessage("RCL_003")).thenReturn(MENSAJE_EXITO);
        when(repositorio.consultarUsuarioPorToken(TOKEN)).thenReturn(Optional.of(USUARIO));
        when(usuarios.consultarUsuarioPorId(USUARIO)).thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                USUARIO, UUID.randomUUID(), 1017223344, "ANA", "PEREZ", "ana.perez@uco.edu.co")));
        when(identidad.buscarCuentaPorIdUsuario(USUARIO)).thenReturn(Optional.of(new CuentaIdentidadDTO(ID_EXTERNO, false)));
    }

    @Test
    void con_enlace_y_clave_validos_fija_la_clave_y_despues_consume_el_enlace() {
        assertEquals(MENSAJE_EXITO, useCase.execute(TOKEN, CLAVE));

        final InOrder orden = inOrder(repositorio, identidad);
        orden.verify(repositorio).consultarUsuarioPorToken(TOKEN);
        orden.verify(identidad).establecerClave(ID_EXTERNO, CLAVE);
        orden.verify(repositorio).consumir(TOKEN);
    }

    @Test
    void token_con_formato_invalido_responde_RCL_002_sin_tocar_la_base_de_datos() {
        for (final String malo : new String[] {null, "", "corto", "A".repeat(42) + "*", "A".repeat(44)}) {
            final ValidationException error = assertThrows(ValidationException.class, () -> useCase.execute(malo, CLAVE));
            assertEquals(MENSAJE_INVALIDO, error.getMessage());
        }
        verifyNoInteractions(repositorio, identidad);
    }

    @Test
    void enlace_que_no_sirve_responde_el_mismo_RCL_002_y_no_cambia_nada() {
        when(repositorio.consultarUsuarioPorToken(TOKEN)).thenReturn(Optional.empty());

        final ValidationException error = assertThrows(ValidationException.class, () -> useCase.execute(TOKEN, CLAVE));

        assertEquals(MENSAJE_INVALIDO, error.getMessage());
        verifyNoInteractions(identidad);
        verify(repositorio, never()).consumir(anyString());
    }

    @Test
    void usuario_del_enlace_sin_ficha_responde_RCL_002() {
        when(usuarios.consultarUsuarioPorId(USUARIO)).thenReturn(Optional.empty());

        assertEquals(MENSAJE_INVALIDO, assertThrows(ValidationException.class, () -> useCase.execute(TOKEN, CLAVE)).getMessage());
        verifyNoInteractions(identidad);
    }

    @Test
    void clave_que_no_cumple_la_politica_se_rechaza_y_el_enlace_sigue_vigente() {
        for (final String debil : new String[] {null, "Ab1-", "sinmayuscula-2026", "SINMINUSCULA-2026", "SinNumero-abc", "SinEspecial2026", "Con Espacio-2026", "1017223344"}) {
            assertThrows(ValidationException.class, () -> useCase.execute(TOKEN, debil), String.valueOf(debil));
        }
        verify(identidad, never()).establecerClave(anyString(), any());
        verify(repositorio, never()).consumir(anyString());
    }

    @Test
    void quien_ya_inicio_sesion_se_ubica_por_su_vinculo_vigente_y_no_por_el_atributo() {
        when(vinculos.findSujetosVigentes(USUARIO)).thenReturn(List.of("sujeto-vinculado"));

        assertEquals(MENSAJE_EXITO, useCase.execute(TOKEN, CLAVE));

        verify(identidad).establecerClave("sujeto-vinculado", CLAVE);
        verify(identidad, never()).buscarCuentaPorIdUsuario(any());
        verify(repositorio).consumir(TOKEN);
    }

    @Test
    void con_varios_vinculos_vigentes_recurre_al_atributo_institucional() {
        when(vinculos.findSujetosVigentes(USUARIO)).thenReturn(List.of("sujeto-a", "sujeto-b"));

        useCase.execute(TOKEN, CLAVE);

        verify(identidad).establecerClave(ID_EXTERNO, CLAVE);
    }

    @Test
    void si_el_proveedor_de_identidad_falla_no_se_consume_el_enlace() {
        doThrow(new IdentityProviderPort.IdentityProviderException("Keycloak no disponible."))
                .when(identidad).establecerClave(ID_EXTERNO, CLAVE);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN, CLAVE));

        verify(repositorio, never()).consumir(anyString());
    }

    @Test
    void sin_cuenta_en_el_proveedor_de_identidad_falla_sin_consumir_el_enlace() {
        when(identidad.buscarCuentaPorIdUsuario(USUARIO)).thenReturn(Optional.empty());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(TOKEN, CLAVE));

        verify(identidad, never()).establecerClave(anyString(), any());
        verify(repositorio, never()).consumir(anyString());
    }
}
