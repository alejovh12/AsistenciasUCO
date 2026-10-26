package co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.AccesoCuentaDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.EstadoAccesoCuenta;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort.IdentityProviderException;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Estado;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccesoCuentaServiceTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID DESTINO = UUID.randomUUID();
    private static final String CORREO = "persona@uco.edu.co";
    private static final String EXTERNO = "kc-1";

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final IdentityProviderPort identity = mock(IdentityProviderPort.class);
    private final AccessProfilePort profiles = mock(AccessProfilePort.class);
    private final UsuarioSincronizacionIdentidadPort desfases = mock(UsuarioSincronizacionIdentidadPort.class);
    private final ExternalIdentityLinkPort links = mock(ExternalIdentityLinkPort.class);
    private final AccesoCuentaService service = new AccesoCuentaService(scope, identity, profiles, desfases, links);

    @BeforeEach
    void escenarioBase() {
        when(scope.findUsuarioIdByEmail(CORREO)).thenReturn(Optional.of(DESTINO));
        when(scope.canGestionarCuenta(ACTOR, DESTINO)).thenReturn(true);
        when(scope.isUsuarioActivo(DESTINO)).thenReturn(true);
        when(identity.buscarCuentaPorIdUsuario(DESTINO)).thenReturn(Optional.of(new CuentaIdentidadDTO(EXTERNO, false)));
        when(profiles.consultar(anyString(), any())).thenReturn(new Resultado(Estado.NO_ASIGNADO, "Perfil no asignado"));
    }

    private void actorAdministrador() {
        when(scope.isAdministradorActivo(ACTOR)).thenReturn(true);
    }

    private void actorCoordinador() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    private void actorDecano() {
        when(scope.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    private static java.util.Set<InstitutionalRole> conjunto(final InstitutionalRole... roles) {
        final java.util.Set<InstitutionalRole> conjunto = EnumSet.noneOf(InstitutionalRole.class);
        conjunto.addAll(java.util.List.of(roles));
        return conjunto;
    }

    private void destinoConRol(final InstitutionalRole... roles) {
        when(scope.findRolesActivosByUsuario(DESTINO)).thenReturn(conjunto(roles));
    }

    private void cuentaConRoles(final InstitutionalRole... roles) {
        when(identity.rolesDeCuenta(EXTERNO)).thenReturn(conjunto(roles));
    }

    private void perfil(final InstitutionalRole rol, final Estado estado) {
        when(profiles.consultar(CORREO, rol)).thenReturn(new Resultado(estado, estado.name()));
    }

    @Test
    void cuenta_nueva_sin_perfil_ni_persona_en_el_pdp_queda_pendiente_sin_acceso() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles(InstitutionalRole.DOCENTE);
        perfil(InstitutionalRole.DOCENTE, Estado.PENDIENTE_PERSONA_NO_REGISTRADA);

        final AccesoCuentaDTO acceso = service.consultar(ACTOR, CORREO);

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, acceso.estado());
        assertTrue(acceso.detalle().contains("DOCENTE"));
    }

    @Test
    void la_cuenta_se_ubica_por_el_vinculo_vigente_aunque_el_atributo_se_repita_en_otras_cuentas() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        when(links.findSujetosVigentes(DESTINO)).thenReturn(java.util.List.of("sub-vinculado"));
        when(identity.buscarCuentaPorIdUsuario(DESTINO)).thenThrow(new IdentityProviderException("ambigua (3 cuentas)"));
        when(identity.rolesDeCuenta("sub-vinculado")).thenReturn(conjunto(InstitutionalRole.DOCENTE));
        perfil(InstitutionalRole.DOCENTE, Estado.ASIGNADO);

        assertEquals(EstadoAccesoCuenta.ACTIVA, service.consultar(ACTOR, CORREO).estado());
        verify(identity, never()).buscarCuentaPorIdUsuario(any());
    }

    @Test
    void sin_vinculo_o_con_varios_emisores_se_usa_la_busqueda_exacta_por_atributo() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles(InstitutionalRole.DOCENTE);
        perfil(InstitutionalRole.DOCENTE, Estado.ASIGNADO);
        when(links.findSujetosVigentes(DESTINO)).thenReturn(java.util.List.of("s1", "s2"));

        assertEquals(EstadoAccesoCuenta.ACTIVA, service.consultar(ACTOR, CORREO).estado());
        verify(identity).buscarCuentaPorIdUsuario(DESTINO);
    }

    @Test
    void cuenta_con_rol_y_perfil_alineados_esta_activa() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DECANO);
        cuentaConRoles(InstitutionalRole.DECANO);
        perfil(InstitutionalRole.DECANO, Estado.ASIGNADO);

        assertEquals(EstadoAccesoCuenta.ACTIVA, service.consultar(ACTOR, CORREO).estado());
    }

    @Test
    void cuenta_con_varios_roles_legitimos_exige_todos_sus_perfiles() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR);
        cuentaConRoles(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR);
        perfil(InstitutionalRole.DOCENTE, Estado.ASIGNADO);
        perfil(InstitutionalRole.COORDINADOR, Estado.NO_ASIGNADO);

        final AccesoCuentaDTO acceso = service.consultar(ACTOR, CORREO);

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, acceso.estado());
        assertEquals(2, acceso.roles().stream().filter(r -> r.esperado()).count());
    }

    @Test
    void desfase_entre_sql_y_proveedor_es_pendiente() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles();
        perfil(InstitutionalRole.DOCENTE, Estado.ASIGNADO);

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, service.consultar(ACTOR, CORREO).estado());
    }

    @Test
    void perfil_revocado_con_rol_inactivo_en_sql_deja_la_cuenta_inactiva() {
        actorAdministrador();
        destinoConRol();
        cuentaConRoles();

        assertEquals(EstadoAccesoCuenta.INACTIVA, service.consultar(ACTOR, CORREO).estado());
    }

    @Test
    void perfil_que_deberia_estar_revocado_o_rol_residual_es_pendiente() {
        actorAdministrador();
        destinoConRol();
        cuentaConRoles(InstitutionalRole.ESTUDIANTE);
        perfil(InstitutionalRole.ESTUDIANTE, Estado.ASIGNADO);

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, service.consultar(ACTOR, CORREO).estado());
    }

    @Test
    void rechazo_controlado_del_pdp_es_problema() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles(InstitutionalRole.DOCENTE);
        perfil(InstitutionalRole.DOCENTE, Estado.RECHAZADO);

        assertEquals(EstadoAccesoCuenta.PROBLEMA, service.consultar(ACTOR, CORREO).estado());
    }

    @Test
    void cuenta_ausente_en_el_proveedor_con_roles_esperados_es_problema() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        when(identity.buscarCuentaPorIdUsuario(DESTINO)).thenReturn(Optional.empty());

        assertEquals(EstadoAccesoCuenta.PROBLEMA, service.consultar(ACTOR, CORREO).estado());
    }

    @Test
    void proveedor_de_identidad_caido_es_pendiente_nunca_acceso() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        when(identity.buscarCuentaPorIdUsuario(DESTINO)).thenThrow(new IdentityProviderException("sin conexion"));

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, service.consultar(ACTOR, CORREO).estado());
    }

    @Test
    void pdp_caido_es_pendiente() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles(InstitutionalRole.DOCENTE);
        perfil(InstitutionalRole.DOCENTE, Estado.PENDIENTE_NO_DISPONIBLE);

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, service.consultar(ACTOR, CORREO).estado());
    }

    // ---- alcance del actor ----

    @Test
    void coordinador_solo_ve_y_gestiona_docente_y_estudiante_nunca_decano_ni_administrador() {
        actorCoordinador();
        destinoConRol(InstitutionalRole.DOCENTE, InstitutionalRole.DECANO);
        cuentaConRoles(InstitutionalRole.DOCENTE, InstitutionalRole.DECANO);
        perfil(InstitutionalRole.DOCENTE, Estado.ASIGNADO);

        final AccesoCuentaDTO acceso = service.consultar(ACTOR, CORREO);

        assertEquals(EstadoAccesoCuenta.ACTIVA, acceso.estado());
        assertTrue(acceso.roles().stream().noneMatch(r -> r.rol().equals("DECANO") || r.rol().equals("ADMINISTRADOR")));
    }

    @Test
    void reconciliacion_de_coordinador_no_asigna_ni_toca_perfiles_de_decano_o_administrador() {
        actorCoordinador();
        destinoConRol(InstitutionalRole.DOCENTE, InstitutionalRole.DECANO);
        cuentaConRoles();
        when(profiles.sincronizar(eq(CORREO), any())).thenReturn(new Resultado(Estado.ASIGNADO, "ok"));
        when(profiles.revocar(eq(CORREO), any())).thenReturn(new Resultado(Estado.NO_ASIGNADO, "ok"));

        service.reconciliar(ACTOR, CORREO);

        verify(identity).asignarRol(EXTERNO, InstitutionalRole.DOCENTE);
        verify(identity, never()).asignarRol(EXTERNO, InstitutionalRole.DECANO);
        verify(identity, never()).asignarRol(EXTERNO, InstitutionalRole.ADMINISTRADOR);
        verify(profiles, never()).sincronizar(CORREO, InstitutionalRole.DECANO);
        verify(profiles, never()).revocar(CORREO, InstitutionalRole.DECANO);
        verify(profiles, never()).sincronizar(CORREO, InstitutionalRole.ADMINISTRADOR);
    }

    @Test
    void decano_alcanza_coordinador_docente_y_estudiante_pero_no_administrador() {
        actorDecano();
        destinoConRol(InstitutionalRole.COORDINADOR);
        cuentaConRoles(InstitutionalRole.COORDINADOR);
        perfil(InstitutionalRole.COORDINADOR, Estado.ASIGNADO);

        final AccesoCuentaDTO acceso = service.consultar(ACTOR, CORREO);

        assertEquals(3, acceso.roles().size());
        assertTrue(acceso.roles().stream().noneMatch(r -> r.rol().equals("ADMINISTRADOR") || r.rol().equals("DECANO")));
    }

    @Test
    void actor_sin_rol_gestor_es_rechazado_antes_de_consultar_a_nadie() {
        assertThrows(ForbiddenException.class, () -> service.consultar(ACTOR, CORREO));
        verify(scope, never()).findUsuarioIdByEmail(anyString());
    }

    @Test
    void cuenta_fuera_del_ambito_del_actor_es_rechazada() {
        actorCoordinador();
        when(scope.canGestionarCuenta(ACTOR, DESTINO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.consultar(ACTOR, CORREO));
        verify(identity, never()).buscarCuentaPorIdUsuario(any());
    }

    @Test
    void correo_desconocido_o_vacio_es_no_encontrado() {
        actorAdministrador();
        when(scope.findUsuarioIdByEmail("otro@uco.edu.co")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.consultar(ACTOR, "otro@uco.edu.co"));
        assertThrows(ResourceNotFoundException.class, () -> service.consultar(ACTOR, " "));
        assertThrows(ResourceNotFoundException.class, () -> service.consultar(ACTOR, null));
    }

    @Test
    void el_correo_se_normaliza_antes_de_resolver_la_cuenta() {
        actorAdministrador();
        destinoConRol();
        cuentaConRoles();

        assertEquals(EstadoAccesoCuenta.INACTIVA, service.consultar(ACTOR, "  PERSONA@UCO.EDU.CO ").estado());
    }

    // ---- reconciliacion ----

    @Test
    void reconciliacion_asigna_rol_y_perfil_faltantes_y_es_idempotente() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles();
        when(profiles.sincronizar(CORREO, InstitutionalRole.DOCENTE)).thenReturn(new Resultado(Estado.ASIGNADO, "ok"));
        when(profiles.revocar(eq(CORREO), any())).thenReturn(new Resultado(Estado.NO_ASIGNADO, "ok"));

        service.reconciliar(ACTOR, CORREO);
        service.reconciliar(ACTOR, CORREO);

        verify(identity, org.mockito.Mockito.times(2)).asignarRol(EXTERNO, InstitutionalRole.DOCENTE);
        verify(profiles, org.mockito.Mockito.times(2)).sincronizar(CORREO, InstitutionalRole.DOCENTE);
        verify(identity, org.mockito.Mockito.times(2)).habilitarCuenta(EXTERNO);
    }

    @Test
    void reconciliacion_revoca_perfil_y_retira_rol_del_usuario_sin_rol_activo_y_deshabilita_si_esta_inactivo() {
        actorAdministrador();
        destinoConRol();
        cuentaConRoles(InstitutionalRole.COORDINADOR);
        when(scope.isUsuarioActivo(DESTINO)).thenReturn(false);
        when(profiles.sincronizar(eq(CORREO), any())).thenReturn(new Resultado(Estado.ASIGNADO, "ok"));
        when(profiles.revocar(eq(CORREO), any())).thenReturn(new Resultado(Estado.NO_ASIGNADO, "ok"));

        service.reconciliar(ACTOR, CORREO);

        verify(identity).retirarRol(EXTERNO, InstitutionalRole.COORDINADOR);
        verify(profiles).revocar(CORREO, InstitutionalRole.COORDINADOR);
        verify(identity).deshabilitarCuenta(EXTERNO);
        verify(profiles, never()).sincronizar(eq(CORREO), any());
    }

    @Test
    void reconciliacion_con_pdp_caido_registra_el_desfase_y_no_concede_acceso() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles(InstitutionalRole.DOCENTE);
        when(profiles.sincronizar(CORREO, InstitutionalRole.DOCENTE))
                .thenReturn(new Resultado(Estado.PENDIENTE_NO_DISPONIBLE, "PDP caido"));
        when(profiles.revocar(eq(CORREO), any())).thenReturn(new Resultado(Estado.NO_ASIGNADO, "ok"));
        perfil(InstitutionalRole.DOCENTE, Estado.PENDIENTE_NO_DISPONIBLE);

        final AccesoCuentaDTO acceso = service.reconciliar(ACTOR, CORREO);

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, acceso.estado());
        verify(desfases).registrar(eq(DESTINO), eq("RECONCILIAR_ACCESO"), anyString());
    }

    @Test
    void reconciliacion_con_proveedor_caido_registra_el_desfase_sin_lanzar() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        when(identity.buscarCuentaPorIdUsuario(DESTINO)).thenThrow(new IdentityProviderException("sin conexion"));
        when(profiles.sincronizar(eq(CORREO), any())).thenReturn(new Resultado(Estado.ASIGNADO, "ok"));
        when(profiles.revocar(eq(CORREO), any())).thenReturn(new Resultado(Estado.NO_ASIGNADO, "ok"));

        final AccesoCuentaDTO acceso = service.reconciliar(ACTOR, CORREO);

        assertEquals(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION, acceso.estado());
        verify(desfases).registrar(eq(DESTINO), eq("RECONCILIAR_ACCESO"), anyString());
    }

    @Test
    void falla_al_registrar_el_desfase_no_oculta_el_resultado() {
        actorAdministrador();
        destinoConRol(InstitutionalRole.DOCENTE);
        cuentaConRoles(InstitutionalRole.DOCENTE);
        when(profiles.sincronizar(CORREO, InstitutionalRole.DOCENTE))
                .thenReturn(new Resultado(Estado.RECHAZADO, "conflicto"));
        when(profiles.revocar(eq(CORREO), any())).thenReturn(new Resultado(Estado.NO_ASIGNADO, "ok"));
        perfil(InstitutionalRole.DOCENTE, Estado.RECHAZADO);
        doThrow(new IllegalStateException("metricas")).when(desfases).registrar(any(), anyString(), anyString());

        assertEquals(EstadoAccesoCuenta.PROBLEMA, service.reconciliar(ACTOR, CORREO).estado());
        assertFalse(service.consultar(ACTOR, CORREO).detalle().isBlank());
    }
}
