package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort.ExternalIdentityConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort.ExternalIdentityLink;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline.ExternalIdentityRejectedException.Reason;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.InstitutionalIdentityAmbiguityException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SEC-BASELINE-017: vínculo estable {@code issuer + subject → Usuario.id}. */
class ExternalIdentityResolverTest {

    private static final String ISSUER = "http://keycloak.localhost:19090/realms/security-baseline";
    private static final String SUBJECT = "6f0a6a1e-0000-4000-8000-000000000001";
    private static final UUID USER_A = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USER_B = UUID.fromString("11111111-e3cd-485c-b275-47e7b731e18c");

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ExternalIdentityLinkPort links = mock(ExternalIdentityLinkPort.class);
    private final ExternalIdentityResolver resolver = new ExternalIdentityResolver(scope, links);

    private Reason rejection(final String issuer, final String subject, final String email, final boolean verified) {
        return assertThrows(ExternalIdentityRejectedException.class,
                () -> resolver.resolve(issuer, subject, email, verified)).reason();
    }

    @Test
    void primera_vinculacion_valida_normaliza_el_correo_y_registra_el_vinculo() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("persona@uco.edu.co")).thenReturn(Optional.of(USER_A));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(true);
        when(links.link(ISSUER, SUBJECT, USER_A, "persona@uco.edu.co")).thenReturn(true);

        final var resolution = resolver.resolve(ISSUER, SUBJECT, "  Persona@UCO.EDU.CO ", true);

        assertEquals(USER_A, resolution.usuarioId());
        assertTrue(resolution.newlyLinked());
        verify(links).link(ISSUER, SUBJECT, USER_A, "persona@uco.edu.co");
    }

    @Test
    void vinculacion_repetida_es_idempotente_y_no_consulta_por_correo() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_A, true)));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(true);

        final var first = resolver.resolve(ISSUER, SUBJECT, "persona@uco.edu.co", true);
        final var second = resolver.resolve(ISSUER, SUBJECT, "persona@uco.edu.co", true);

        assertEquals(USER_A, first.usuarioId());
        assertEquals(USER_A, second.usuarioId());
        assertFalse(second.newlyLinked());
        verify(scope, never()).findUsuarioIdByEmail(anyString());
        verify(links, never()).link(anyString(), anyString(), any(), anyString());
    }

    @Test
    void usuario_inexistente_se_rechaza_sin_registrar_vinculo() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("nadie@uco.edu.co")).thenReturn(Optional.empty());

        assertEquals(Reason.USER_UNKNOWN, rejection(ISSUER, SUBJECT, "nadie@uco.edu.co", true));
        verify(links, never()).link(anyString(), anyString(), any(), anyString());
    }

    @Test
    void correo_duplicado_es_ambiguo_y_no_elige_una_cuenta() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("dup@uco.edu.co")).thenThrow(new InstitutionalIdentityAmbiguityException());

        assertEquals(Reason.USER_AMBIGUOUS, rejection(ISSUER, SUBJECT, "dup@uco.edu.co", true));
        verify(links, never()).link(anyString(), anyString(), any(), anyString());
    }

    @Test
    void correo_no_verificado_o_ausente_no_vincula_ni_consulta_usuarios() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());

        assertEquals(Reason.EMAIL_NOT_VERIFIED, rejection(ISSUER, SUBJECT, "persona@uco.edu.co", false));
        assertEquals(Reason.EMAIL_MISSING, rejection(ISSUER, SUBJECT, "  ", true));
        assertEquals(Reason.EMAIL_MISSING, rejection(ISSUER, SUBJECT, null, true));
        verify(scope, never()).findUsuarioIdByEmail(anyString());
    }

    @Test
    void usuario_inactivo_no_se_vincula() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("baja@uco.edu.co")).thenReturn(Optional.of(USER_A));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(false);

        assertEquals(Reason.USER_INACTIVE, rejection(ISSUER, SUBJECT, "baja@uco.edu.co", true));
        verify(links, never()).link(anyString(), anyString(), any(), anyString());
    }

    @Test
    void desactivacion_posterior_del_usuario_vinculado_rechaza_aunque_el_vinculo_siga_vigente() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_A, true)));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(false);

        assertEquals(Reason.USER_INACTIVE, rejection(ISSUER, SUBJECT, "persona@uco.edu.co", true));
    }

    @Test
    void cambio_de_correo_en_el_idp_conserva_la_cuenta_vinculada_y_no_apropia_otra() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_A, true)));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(true);
        // El nuevo correo del IdP coincide con OTRA cuenta institucional: no debe usarse.
        when(scope.findUsuarioIdByEmail("otra@uco.edu.co")).thenReturn(Optional.of(USER_B));

        assertEquals(USER_A, resolver.resolve(ISSUER, SUBJECT, "otra@uco.edu.co", true).usuarioId());
        verify(scope, never()).findUsuarioIdByEmail(anyString());
    }

    @Test
    void cambio_de_subject_con_el_mismo_correo_no_reasigna_si_la_cuenta_ya_tiene_otra_identidad() {
        final String nuevoSubject = "6f0a6a1e-0000-4000-8000-000000000002";
        when(links.find(ISSUER, nuevoSubject)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("persona@uco.edu.co")).thenReturn(Optional.of(USER_A));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(true);
        when(links.link(ISSUER, nuevoSubject, USER_A, "persona@uco.edu.co"))
                .thenThrow(new ExternalIdentityConflictException("ocupada"));

        assertEquals(Reason.LINK_CONFLICT, rejection(ISSUER, nuevoSubject, "persona@uco.edu.co", true));
    }

    @Test
    void dos_identidades_intentando_ocupar_la_misma_cuenta_la_segunda_se_rechaza() {
        final String segundoSubject = "6f0a6a1e-0000-4000-8000-000000000003";
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.empty());
        when(links.find(ISSUER, segundoSubject)).thenReturn(Optional.empty());
        when(scope.findUsuarioIdByEmail("persona@uco.edu.co")).thenReturn(Optional.of(USER_A));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(true);
        when(links.link(ISSUER, SUBJECT, USER_A, "persona@uco.edu.co")).thenReturn(true);
        when(links.link(ISSUER, segundoSubject, USER_A, "persona@uco.edu.co"))
                .thenThrow(new ExternalIdentityConflictException("cuenta ya vinculada"));

        assertEquals(USER_A, resolver.resolve(ISSUER, SUBJECT, "persona@uco.edu.co", true).usuarioId());
        assertEquals(Reason.LINK_CONFLICT, rejection(ISSUER, segundoSubject, "persona@uco.edu.co", true));
    }

    @Test
    void una_identidad_vinculada_a_una_cuenta_no_ocupa_una_segunda() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_A, true)));
        when(scope.isUsuarioActivo(USER_A)).thenReturn(true);

        // Aunque el token apunte (por correo) a la cuenta B, la identidad sigue resolviendo A.
        assertEquals(USER_A, resolver.resolve(ISSUER, SUBJECT, "b@uco.edu.co", true).usuarioId());
        verify(links, never()).link(anyString(), anyString(), any(), anyString());
    }

    @Test
    void vinculo_revocado_se_rechaza_aunque_el_correo_verificado_coincida() {
        when(links.find(ISSUER, SUBJECT)).thenReturn(Optional.of(new ExternalIdentityLink(USER_A, false)));

        assertEquals(Reason.LINK_REVOKED, rejection(ISSUER, SUBJECT, "persona@uco.edu.co", true));
        verify(scope, never()).findUsuarioIdByEmail(anyString());
        verify(links, never()).link(anyString(), anyString(), any(), anyString());
    }

    @Test
    void issuer_o_subject_ausentes_se_rechazan() {
        assertEquals(Reason.MISSING_ISSUER, rejection(null, SUBJECT, "persona@uco.edu.co", true));
        assertEquals(Reason.MISSING_ISSUER, rejection(" ", SUBJECT, "persona@uco.edu.co", true));
        assertEquals(Reason.MISSING_SUBJECT, rejection(ISSUER, null, "persona@uco.edu.co", true));
        assertEquals(Reason.MISSING_SUBJECT, rejection(ISSUER, "", "persona@uco.edu.co", true));
    }
}
