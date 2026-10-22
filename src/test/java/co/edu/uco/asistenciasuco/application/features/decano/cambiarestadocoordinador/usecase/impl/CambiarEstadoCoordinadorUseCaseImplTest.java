package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.domain.CambiarEstadoCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU122 (CONTRACT HU121-HU122 §3.2): activar o desactivar al coordinador. La identidad se sincroniza despues del commit.
 * Depende de IAM-001 ({@link SincronizarCuentaInstitucionalService}): sin ese servicio estas pruebas no compilan (RED).
 */
class CambiarEstadoCoordinadorUseCaseImplTest {

    private static final UUID DECANO = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();
    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID USUARIO_COORDINADOR = UUID.randomUUID();
    private static final String MENSAJE_IAM_002 = "El cambio se guardo. La cuenta de acceso se sincronizara pronto.";

    private final CoordinadorCommandPort commandPort = mock(CoordinadorCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SincronizarCuentaInstitucionalService sincronizacion = mock(SincronizarCuentaInstitucionalService.class);
    private final MessageCatalogPort messageCatalog = mock(MessageCatalogPort.class);
    private final CambiarEstadoCoordinadorUseCaseImpl useCase =
            new CambiarEstadoCoordinadorUseCaseImpl(commandPort, scopePort, sincronizacion, messageCatalog);

    private void facultadResuelta() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(FACULTAD));
    }

    private void dbCambiaEstado(final boolean estaActivo, final boolean personaSinRolesActivos) {
        facultadResuelta();
        when(commandPort.cambiarEstadoCoordinador(COORDINADOR, estaActivo, FACULTAD, DECANO))
                .thenReturn(new ResultadoGestionCoordinadorProjection(USUARIO_COORDINADOR, personaSinRolesActivos));
    }

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_sin_tocar_db_ni_identidad_cuando_no_hay_facultad_de_decano() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new CambiarEstadoCoordinadorDomain(COORDINADOR, false, DECANO)));

        verifyNoInteractions(commandPort, sincronizacion);
    }

    @Test
    void execute_desactivar_persona_sin_otros_roles_deshabilita_la_cuenta() {
        dbCambiaEstado(false, true);
        when(sincronizacion.desactivar(eq(USUARIO_COORDINADOR), eq(InstitutionalRole.COORDINADOR), eq(true)))
                .thenReturn(ResultadoSincronizacion.OK);

        final ResultadoGestionCoordinadorDomain resultado =
                useCase.execute(new CambiarEstadoCoordinadorDomain(COORDINADOR, false, DECANO));

        verify(sincronizacion).desactivar(eq(USUARIO_COORDINADOR), eq(InstitutionalRole.COORDINADOR), eq(true));
        verify(sincronizacion, never()).reactivar(any(), any());
        assertEquals(COORDINADOR, resultado.idCoordinador());
        assertFalse(resultado.sincronizacionPendiente());
    }

    @Test
    void execute_desactivar_persona_con_otro_rol_activo_no_deshabilita_la_cuenta() {
        dbCambiaEstado(false, false);
        when(sincronizacion.desactivar(eq(USUARIO_COORDINADOR), eq(InstitutionalRole.COORDINADOR), eq(false)))
                .thenReturn(ResultadoSincronizacion.OK);

        useCase.execute(new CambiarEstadoCoordinadorDomain(COORDINADOR, false, DECANO));

        verify(sincronizacion).desactivar(eq(USUARIO_COORDINADOR), eq(InstitutionalRole.COORDINADOR), eq(false));
    }

    @Test
    void execute_reactivar_habilita_la_cuenta_y_reasigna_el_rol() {
        dbCambiaEstado(true, false);
        when(sincronizacion.reactivar(eq(USUARIO_COORDINADOR), eq(InstitutionalRole.COORDINADOR)))
                .thenReturn(ResultadoSincronizacion.OK);

        useCase.execute(new CambiarEstadoCoordinadorDomain(COORDINADOR, true, DECANO));

        verify(sincronizacion).reactivar(eq(USUARIO_COORDINADOR), eq(InstitutionalRole.COORDINADOR));
        verify(sincronizacion, never()).desactivar(any(), any(), anyBoolean());
    }

    @Test
    void execute_con_identidad_pendiente_responde_exito_con_advertencia_iam_002() {
        dbCambiaEstado(false, true);
        when(sincronizacion.desactivar(eq(USUARIO_COORDINADOR), eq(InstitutionalRole.COORDINADOR), eq(true)))
                .thenReturn(ResultadoSincronizacion.PENDIENTE);
        when(messageCatalog.getUserMessage("IAM_002")).thenReturn(MENSAJE_IAM_002);

        final ResultadoGestionCoordinadorDomain resultado =
                useCase.execute(new CambiarEstadoCoordinadorDomain(COORDINADOR, false, DECANO));

        assertTrue(resultado.sincronizacionPendiente());
        assertEquals(MENSAJE_IAM_002, resultado.mensajeUsuario());
    }

    @Test
    void execute_no_invoca_la_identidad_si_la_db_rechaza_por_programa_activo() {
        facultadResuelta();
        when(commandPort.cambiarEstadoCoordinador(COORDINADOR, false, FACULTAD, DECANO))
                .thenThrow(new ConflictException("COO_001"));

        assertThrows(ConflictException.class,
                () -> useCase.execute(new CambiarEstadoCoordinadorDomain(COORDINADOR, false, DECANO)));

        verifyNoInteractions(sincronizacion);
    }
}
