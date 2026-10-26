package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionarPlanEstudioUseCaseImplTest {

    private final PlanEstudioCommandPort commandPort = mock(PlanEstudioCommandPort.class);
    private final PlanEstudioQueryPort queryPort = mock(PlanEstudioQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final GestionarPlanEstudioUseCaseImpl useCase = new GestionarPlanEstudioUseCaseImpl(commandPort, queryPort, scopePort);

    @Test
    void guardar_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.guardar(null));
    }

    @Test
    void guardar_lanza_forbidden_cuando_no_resuelve_programa_del_coordinador() {
        final UUID usuario = UUID.randomUUID();
        final PlanEstudioDomain domain = new PlanEstudioDomain(null, 2026, usuario);
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.guardar(domain));
    }

    @Test
    void guardar_con_idPlanEstudio_nulo_genera_uno_nuevo() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final PlanEstudioDomain domain = new PlanEstudioDomain(null, 2026, usuario);
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.of(programa));

        useCase.guardar(domain);

        final ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).registrarOActualizarPlanEstudio(idCaptor.capture(), org.mockito.ArgumentMatchers.eq(programa),
                org.mockito.ArgumentMatchers.eq(2026), org.mockito.ArgumentMatchers.eq(usuario));
        assertNotNull(idCaptor.getValue());
    }

    @Test
    void guardar_con_idPlanEstudio_existente_lo_conserva() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID planId = UUID.randomUUID();
        final PlanEstudioDomain domain = new PlanEstudioDomain(planId, 2027, usuario);
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.of(programa));

        useCase.guardar(domain);

        verify(commandPort).registrarOActualizarPlanEstudio(planId, programa, 2027, usuario);
    }

    @Test
    void guardar_propaga_el_coordinador_autenticado_como_ejecutor_para_que_la_db_revalide_la_autorizacion() {
        final UUID coordinador = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(coordinador)).thenReturn(Optional.of(programa));

        useCase.guardar(new PlanEstudioDomain(UUID.randomUUID(), 2028, coordinador));

        final ArgumentCaptor<UUID> ejecutor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).registrarOActualizarPlanEstudio(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(programa),
                org.mockito.ArgumentMatchers.eq(2028), ejecutor.capture());
        assertEquals(coordinador, ejecutor.getValue());
    }

    @Test
    void cambiarEstado_inactiva_un_plan_activo_del_programa_del_coordinador() {
        final UUID coordinador = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(coordinador)).thenReturn(Optional.of(programa));
        when(queryPort.consultarPlanesPorPrograma(programa)).thenReturn(List.of(plan(plan, programa, true)));

        useCase.cambiarEstado(plan, coordinador);

        verify(commandPort).cambiarEstadoPlanEstudio(plan, false, coordinador);
    }

    @Test
    void cambiarEstado_activa_un_plan_inactivo() {
        final UUID coordinador = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(coordinador)).thenReturn(Optional.of(programa));
        when(queryPort.consultarPlanesPorPrograma(programa)).thenReturn(List.of(plan(plan, programa, false)));

        useCase.cambiarEstado(plan, coordinador);

        verify(commandPort).cambiarEstadoPlanEstudio(plan, true, coordinador);
    }

    @Test
    void cambiarEstado_de_un_plan_fuera_del_programa_no_lo_encuentra_ni_ejecuta_el_comando() {
        final UUID coordinador = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(coordinador)).thenReturn(Optional.of(programa));
        when(queryPort.consultarPlanesPorPrograma(programa)).thenReturn(List.of(plan(UUID.randomUUID(), programa, true)));

        assertThrows(ResourceNotFoundException.class, () -> useCase.cambiarEstado(UUID.randomUUID(), coordinador));
        verifyNoInteractions(commandPort);
    }

    @Test
    void comandos_de_plan_exigen_un_coordinador_con_programa() {
        final UUID usuario = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.cambiarEstado(plan, usuario));
        assertThrows(ForbiddenException.class, () -> useCase.agregarSemestre(plan, usuario));
        assertThrows(ForbiddenException.class, () -> useCase.quitarSemestre(plan, 2, usuario));
        verifyNoInteractions(commandPort);
    }

    @Test
    void agregarSemestre_y_quitarSemestre_delegan_con_el_coordinador_como_ejecutor() {
        final UUID coordinador = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(coordinador)).thenReturn(Optional.of(UUID.randomUUID()));

        useCase.agregarSemestre(plan, coordinador);
        useCase.quitarSemestre(plan, 9, coordinador);

        verify(commandPort).agregarSemestrePlanEstudio(plan, coordinador);
        verify(commandPort).quitarSemestrePlanEstudio(plan, 9, coordinador);
    }

    @Test
    void quitarSemestre_rechaza_numero_nulo_o_no_positivo_sin_llamar_a_la_db() {
        final UUID coordinador = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();

        assertThrows(ValidationException.class, () -> useCase.quitarSemestre(plan, null, coordinador));
        assertThrows(ValidationException.class, () -> useCase.quitarSemestre(plan, 0, coordinador));
        verifyNoInteractions(commandPort);
    }

    private static PlanEstudioProjection plan(final UUID id, final UUID programa, final boolean activo) {
        return new PlanEstudioProjection(id, programa, "Ingenieria", "2026", activo, activo ? "SI" : "NO", null, 10);
    }
}
