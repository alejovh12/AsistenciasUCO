package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoEstadoAsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.admin.exception.CatalogoErrorCode;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.EstadoAsignaturaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GestionarAsignaturaUseCaseImplTest {

    private final AsignaturaCommandPort commandPort = mock(AsignaturaCommandPort.class);
    private final AsignaturaQueryPort queryPort = mock(AsignaturaQueryPort.class);
    private final PlanEstudioQueryPort planQueryPort = mock(PlanEstudioQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final MessageCatalogPort mensajes = mock(MessageCatalogPort.class);
    private final GestionarAsignaturaUseCaseImpl useCase = new GestionarAsignaturaUseCaseImpl(
            commandPort, queryPort, planQueryPort, scopePort, mensajes);

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAM = UUID.randomUUID();

    @Test
    void crear_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.crear(null));
    }

    @Test
    void crear_invoca_command_port_con_datos_del_dominio() {
        final UUID plan = UUID.randomUUID();
        final UUID usuarioEjecutor = ACTOR;
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuarioEjecutor)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));
        final AsignaturaDomain domain = new AsignaturaDomain(
                null, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", null, usuarioEjecutor
        );

        useCase.crear(domain);

        verify(commandPort).crearAsignatura(any(), eq("COD1"), eq("Calculo"), eq(3), eq(plan), eq(1),
                eq("Area"), eq("Componente"), isNull(), isNull(), eq(usuarioEjecutor));
    }

    @Test
    void SES_GEN_01_crear_propaga_horas_semanales_al_puerto() {
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));

        useCase.crear(new AsignaturaDomain(null, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", null, ACTOR, 4));

        verify(commandPort).crearAsignatura(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("COD1"),
                org.mockito.ArgumentMatchers.eq("Calculo"), org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(plan), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("Area"), org.mockito.ArgumentMatchers.eq("Componente"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(4), org.mockito.ArgumentMatchers.eq(ACTOR));
    }

    @Test
    void SES_GEN_01_actualizar_propaga_horas_semanales_al_puerto() {
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));
        when(queryPort.consultarAsignaturasPorPlan(plan)).thenReturn(List.of(subject(id, plan, PROGRAM)));

        useCase.actualizar(new AsignaturaDomain(id, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", null, ACTOR, 6));

        verify(commandPort).actualizarAsignatura(eq(id), eq("COD1"), eq("Calculo"), eq(3), eq(plan), eq(1),
                eq("Area"), eq("Componente"), isNull(), eq(6), eq(ACTOR));
    }

    @Test
    void actualizar_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.actualizar(null));
    }

    @Test
    void actualizar_invoca_command_port_con_id_de_asignatura_y_actor_autorizado() {
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));
        when(queryPort.consultarAsignaturasPorPlan(plan)).thenReturn(List.of(subject(id, plan, PROGRAM)));
        final AsignaturaDomain domain = new AsignaturaDomain(id, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", null, ACTOR);

        useCase.actualizar(domain);

        final ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).actualizarAsignatura(idCaptor.capture(), eq("COD1"), eq("Calculo"), eq(3), eq(plan), eq(1),
                eq("Area"), eq("Componente"), isNull(), isNull(), eq(ACTOR));
        assertEquals(id, idCaptor.getValue());
    }

    @Test
    void actualizar_rechaza_asignatura_de_otro_programa_sin_invocar_command() {
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));
        when(queryPort.consultarAsignaturasPorPlan(plan)).thenReturn(List.of(subject(id, plan, UUID.randomUUID())));

        final var asignaturaDomain2 = new AsignaturaDomain(id, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", null, ACTOR);
        assertThrows(ForbiddenException.class, () -> useCase.actualizar(asignaturaDomain2));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void crear_rechaza_plan_de_otro_programa_sin_invocar_command() {
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, UUID.randomUUID())));

        final var asignaturaDomain = new AsignaturaDomain(null, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", null, ACTOR);
        assertThrows(ForbiddenException.class, () -> useCase.crear(asignaturaDomain));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void toggleEstado_rechaza_id_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.toggleEstado(null, null, ACTOR));
    }

    @Test
    void toggleEstado_invoca_command_port_con_actor_autorizado() {
        final UUID id = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(queryPort.consultarAsignaturasPorPrograma(PROGRAM)).thenReturn(List.of(subject(id, UUID.randomUUID(), PROGRAM)));

        when(commandPort.toggleEstadoAsignatura(id, null, ACTOR)).thenReturn(resultado("Texto.", false));

        useCase.toggleEstado(id, null, ACTOR);

        verify(commandPort).toggleEstadoAsignatura(id, null, ACTOR);
    }

    @Test
    void toggleEstado_rechaza_asignatura_fuera_del_programa_del_actor() {
        final UUID id = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(queryPort.consultarAsignaturasPorPrograma(PROGRAM)).thenReturn(List.of(subject(UUID.randomUUID(), UUID.randomUUID(), PROGRAM)));

        assertThrows(ForbiddenException.class, () -> useCase.toggleEstado(id, null, ACTOR));

        verifyNoInteractions(commandPort);
    }

    @Test
    void cualquier_mutacion_rechaza_actor_no_resuelto() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(null)).thenReturn(Optional.empty());

        final var idAleatorio = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () -> useCase.toggleEstado(idAleatorio, null, null));

        verifyNoInteractions(commandPort);
    }

    // HU074: el resultado trae el estado que confirma el codigo de exito del SP y el mensaje que entrego la BD.
    @Test
    void hu074_toggleEstado_de_activa_queda_inactiva_con_el_mensaje_de_la_bd() {
        final UUID id = autorizar(true);
        when(commandPort.toggleEstadoAsignatura(id, null, ACTOR)).thenReturn(resultado("  La asignatura quedó inactiva. ", false));

        final EstadoAsignaturaDomain resultado = useCase.toggleEstado(id, null, ACTOR);

        assertEquals(id, resultado.idAsignatura());
        assertFalse(resultado.estaActivaAsignatura());
        assertEquals("La asignatura quedó inactiva.", resultado.mensajeUsuario());
        verifyNoInteractions(mensajes);
    }

    // El código confirmado por el SP prevalece sobre la lectura previa y sobre el estado pedido.
    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "true,  NULL,  true,  true",
            "false, NULL,  false, false",
            "false, true,  true,  true",
            "true,  false, false, false",
            "true,  true,  true,  true",
            "false, false, false, false"
    })
    void hu074_toggleEstado_toma_el_estado_del_codigo_de_exito_del_sp(final boolean leida, final Boolean deseada,
                                                                       final Boolean confirmado, final boolean esperada) {
        final UUID id = autorizar(leida);
        when(commandPort.toggleEstadoAsignatura(id, deseada, ACTOR)).thenReturn(resultado("Texto.", confirmado));

        assertEquals(esperada, useCase.toggleEstado(id, deseada, ACTOR).estaActivaAsignatura());
        verify(commandPort).toggleEstadoAsignatura(id, deseada, ACTOR);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "true,  NULL,  false",
            "false, NULL,  true",
            "true,  true,  true",
            "false, false, false",
            "true,  false, false"
    })
    void hu074_toggleEstado_sin_codigo_del_sp_usa_el_estado_pedido_o_el_contrario_al_leido(
            final boolean leida, final Boolean deseada, final boolean esperada) {
        final UUID id = autorizar(leida);
        when(commandPort.toggleEstadoAsignatura(id, deseada, ACTOR)).thenReturn(resultado("Texto.", null));

        assertEquals(esperada, useCase.toggleEstado(id, deseada, ACTOR).estaActivaAsignatura());
    }

    @Test
    void hu074_toggleEstado_sin_mensaje_de_la_bd_usa_el_catalogo_asi_011() {
        final UUID id = autorizar(false);
        when(commandPort.toggleEstadoAsignatura(id, null, ACTOR)).thenReturn(resultado(" ", true));
        when(mensajes.findUserMessage("ASI_011")).thenReturn(Optional.of("Texto del catálogo."));

        final EstadoAsignaturaDomain resultado = useCase.toggleEstado(id, null, ACTOR);

        assertTrue(resultado.estaActivaAsignatura());
        assertEquals("Texto del catálogo.", resultado.mensajeUsuario());
    }

    @Test
    void hu074_toggleEstado_sin_mensaje_ni_catalogo_usa_el_respaldo_de_asi_012() {
        final UUID id = autorizar(true);
        when(commandPort.toggleEstadoAsignatura(id, null, ACTOR)).thenReturn(resultado(null, null));
        when(mensajes.findUserMessage("ASI_012")).thenReturn(Optional.empty());

        assertEquals("La asignatura quedó inactiva.", useCase.toggleEstado(id, null, ACTOR).mensajeUsuario());
    }

    @Test
    void hu074_toggleEstado_propaga_el_rechazo_asi_009_de_la_bd() {
        final UUID id = autorizar(true);
        when(commandPort.toggleEstadoAsignatura(id, false, ACTOR)).thenThrow(new ConflictException(CatalogoErrorCode.ASI_009));

        final ConflictException exception = assertThrows(ConflictException.class, () -> useCase.toggleEstado(id, false, ACTOR));

        assertEquals("ASI_009", exception.getCode());
    }

    private static ResultadoEstadoAsignaturaProjection resultado(final String mensaje, final Boolean estaActivaResultante) {
        return new ResultadoEstadoAsignaturaProjection(mensaje, estaActivaResultante);
    }

    private UUID autorizar(final boolean estaActiva) {
        final UUID id = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(queryPort.consultarAsignaturasPorPrograma(PROGRAM)).thenReturn(List.of(new AsignaturaProjection(id, "A01", "Calculo", 3,
                UUID.randomUUID(), "Area", UUID.randomUUID(), "Componente", UUID.randomUUID(), UUID.randomUUID(), PROGRAM, "Programa",
                "01", estaActiva, estaActiva ? "SI" : "NO")));
        return id;
    }

    private static PlanEstudioProjection plan(final UUID id, final UUID program) {
        return new PlanEstudioProjection(id, program, "Programa", "INP", true, "SI", null);
    }

    private static AsignaturaProjection subject(final UUID id, final UUID plan, final UUID program) {
        return new AsignaturaProjection(id, "A01", "Calculo", 3, UUID.randomUUID(), "Area",
                UUID.randomUUID(), "Componente", UUID.randomUUID(), plan, program, "Programa", "01", true, "SI");
    }
}
