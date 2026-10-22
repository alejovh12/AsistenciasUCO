package co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * IAM-001 §3 (consumido por HU121/HU122): sincroniza la cuenta de identidad DESPUES del commit en DB.
 * La DB es la fuente de verdad: si el proveedor falla, se registra el desfase y se devuelve PENDIENTE sin propagar.
 */
class SincronizarCuentaInstitucionalServiceTest {

    private static final UUID ID_USUARIO = UUID.randomUUID();
    private static final String ID_EXTERNO = "kc-user-1";
    private static final InstitutionalRole ROL = InstitutionalRole.COORDINADOR;

    private final IdentityProviderPort identityProvider = mock(IdentityProviderPort.class);
    private final UsuarioSincronizacionIdentidadPort desfases = mock(UsuarioSincronizacionIdentidadPort.class);
    private final SincronizarCuentaInstitucionalService service =
            new SincronizarCuentaInstitucionalService(identityProvider, desfases);

    private void cuentaExiste() {
        when(identityProvider.buscarCuentaPorIdUsuario(ID_USUARIO))
                .thenReturn(Optional.of(new CuentaIdentidadDTO(ID_EXTERNO, false)));
    }

    // ---------------------------------------------------------------- desactivar

    @Test
    void desactivar_persona_sin_otros_roles_retira_el_rol_y_deshabilita_la_cuenta() {
        cuentaExiste();

        final ResultadoSincronizacion resultado = service.desactivar(ID_USUARIO, ROL, true);

        assertEquals(ResultadoSincronizacion.OK, resultado);
        final InOrder orden = inOrder(identityProvider);
        orden.verify(identityProvider).retirarRol(ID_EXTERNO, ROL);
        orden.verify(identityProvider).deshabilitarCuenta(ID_EXTERNO);
        verifyNoMoreInteractions(desfases);
    }

    @Test
    void desactivar_persona_con_otro_rol_activo_solo_retira_el_rol() {
        cuentaExiste();

        final ResultadoSincronizacion resultado = service.desactivar(ID_USUARIO, ROL, false);

        assertEquals(ResultadoSincronizacion.OK, resultado);
        verify(identityProvider).retirarRol(ID_EXTERNO, ROL);
        verify(identityProvider, never()).deshabilitarCuenta(anyString());
    }

    // ---------------------------------------------------------------- reactivar

    @Test
    void reactivar_habilita_la_cuenta_y_reasigna_el_rol() {
        cuentaExiste();

        final ResultadoSincronizacion resultado = service.reactivar(ID_USUARIO, ROL);

        assertEquals(ResultadoSincronizacion.OK, resultado);
        final InOrder orden = inOrder(identityProvider);
        orden.verify(identityProvider).habilitarCuenta(ID_EXTERNO);
        orden.verify(identityProvider).asignarRol(ID_EXTERNO, ROL);
    }

    // ---------------------------------------------------------------- actualizarDatos

    @Test
    void actualizar_datos_envia_los_datos_a_la_cuenta_existente() {
        cuentaExiste();
        final ActualizarCuentaIdentidadDTO datos = new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Perez");

        final ResultadoSincronizacion resultado = service.actualizarDatos(ID_USUARIO, datos);

        assertEquals(ResultadoSincronizacion.OK, resultado);
        verify(identityProvider).actualizarDatosCuenta(ID_EXTERNO, datos);
    }

    // ---------------------------------------------------------------- desfases

    @Test
    void cuenta_inexistente_en_el_idp_registra_el_desfase_y_devuelve_pendiente_sin_mas_llamadas() {
        when(identityProvider.buscarCuentaPorIdUsuario(ID_USUARIO)).thenReturn(Optional.empty());

        final ResultadoSincronizacion resultado = service.desactivar(ID_USUARIO, ROL, true);

        assertEquals(ResultadoSincronizacion.PENDIENTE, resultado);
        verify(desfases).registrar(eq(ID_USUARIO), eq("DESACTIVAR"), anyString());
        verify(identityProvider, never()).retirarRol(anyString(), any());
        verify(identityProvider, never()).deshabilitarCuenta(anyString());
    }

    @Test
    void falla_del_idp_al_buscar_registra_el_desfase_de_la_operacion_y_devuelve_pendiente() {
        when(identityProvider.buscarCuentaPorIdUsuario(ID_USUARIO))
                .thenThrow(new IdentityProviderPort.IdentityProviderException("Keycloak no responde"));

        final ResultadoSincronizacion resultado = service.reactivar(ID_USUARIO, ROL);

        assertEquals(ResultadoSincronizacion.PENDIENTE, resultado);
        verify(desfases).registrar(ID_USUARIO, "REACTIVAR", "Keycloak no responde");
    }

    @Test
    void falla_del_idp_a_mitad_de_la_operacion_registra_el_desfase_y_devuelve_pendiente() {
        cuentaExiste();
        doThrow(new IdentityProviderPort.IdentityProviderException("HTTP 500")).when(identityProvider).deshabilitarCuenta(ID_EXTERNO);

        final ResultadoSincronizacion resultado = service.desactivar(ID_USUARIO, ROL, true);

        assertEquals(ResultadoSincronizacion.PENDIENTE, resultado);
        verify(desfases).registrar(ID_USUARIO, "DESACTIVAR", "HTTP 500");
    }

    @Test
    void falla_al_actualizar_datos_registra_el_desfase_actualizar() {
        cuentaExiste();
        final ActualizarCuentaIdentidadDTO datos = new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Perez");
        doThrow(new IdentityProviderPort.IdentityProviderException("conflicto de correo"))
                .when(identityProvider).actualizarDatosCuenta(ID_EXTERNO, datos);

        final ResultadoSincronizacion resultado = service.actualizarDatos(ID_USUARIO, datos);

        assertEquals(ResultadoSincronizacion.PENDIENTE, resultado);
        verify(desfases).registrar(ID_USUARIO, "ACTUALIZAR", "conflicto de correo");
    }

    @Test
    void operacion_no_soportada_por_el_proveedor_se_trata_como_desfase() {
        cuentaExiste();
        doThrow(new IdentityProviderPort.IdentityProviderException("Operacion no soportada por este proveedor."))
                .when(identityProvider).retirarRol(ID_EXTERNO, ROL);

        assertEquals(ResultadoSincronizacion.PENDIENTE, service.desactivar(ID_USUARIO, ROL, false));
        verify(desfases).registrar(eq(ID_USUARIO), eq("DESACTIVAR"), anyString());
    }

    @Test
    void si_el_registro_del_desfase_falla_no_se_propaga_y_el_resultado_sigue_siendo_pendiente() {
        when(identityProvider.buscarCuentaPorIdUsuario(ID_USUARIO)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("tabla no disponible")).when(desfases).registrar(any(), anyString(), anyString());

        assertEquals(ResultadoSincronizacion.PENDIENTE, service.desactivar(ID_USUARIO, ROL, true));
    }
}
