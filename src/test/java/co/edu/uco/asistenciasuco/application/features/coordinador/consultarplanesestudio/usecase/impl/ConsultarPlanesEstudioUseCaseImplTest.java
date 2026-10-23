package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Lectura de planes de estudio por rol: el coordinador ve los de su programa y el decano los de su facultad.
 * Hallazgo del recorrido de vistas: el decano recibia 403 en /coordinador/planes-estudio porque el caso de uso solo
 * resolvia el ambito de un coordinador.
 */
class ConsultarPlanesEstudioUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final PlanEstudioQueryPort queryPort = mock(PlanEstudioQueryPort.class);
    private final ConsultarPlanesEstudioUseCaseImpl useCase = new ConsultarPlanesEstudioUseCaseImpl(scopePort, queryPort);

    private static PlanEstudioProjection plan(final UUID idPrograma) {
        return new PlanEstudioProjection(UUID.randomUUID(), idPrograma, "Ingenieria de Sistemas", "123", true, "SI", null);
    }

    @Test
    void coordinador_consulta_los_planes_de_su_programa() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarPlanesPorPrograma(PROGRAMA)).thenReturn(List.of(plan(PROGRAMA)));

        final List<PlanEstudioDomain> resultado = useCase.execute(ACTOR);

        assertEquals(1, resultado.size());
        verify(queryPort, never()).consultarPlanesPorFacultad(any());
    }

    @Test
    void decano_consulta_los_planes_de_los_programas_de_su_facultad() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
        when(queryPort.consultarPlanesPorFacultad(FACULTAD)).thenReturn(List.of(plan(UUID.randomUUID()), plan(UUID.randomUUID())));

        final List<PlanEstudioDomain> resultado = useCase.execute(ACTOR);

        assertEquals(2, resultado.size());
        verify(queryPort, never()).consultarPlanesPorPrograma(any());
    }

    @Test
    void si_es_coordinador_y_decano_prevalece_el_programa() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
        when(queryPort.consultarPlanesPorPrograma(PROGRAMA)).thenReturn(List.of());

        useCase.execute(ACTOR);

        verify(queryPort).consultarPlanesPorPrograma(PROGRAMA);
        verify(queryPort, never()).consultarPlanesPorFacultad(any());
    }

    @Test
    void sin_programa_ni_facultad_se_rechaza_sin_consultar() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR));

        verifyNoInteractions(queryPort);
    }
}
