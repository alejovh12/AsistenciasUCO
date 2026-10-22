package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
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

/** Lectura de asignaturas por rol: coordinador por programa, decano por facultad. */
class ConsultarAsignaturasUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final AsignaturaQueryPort queryPort = mock(AsignaturaQueryPort.class);
    private final ConsultarAsignaturasUseCaseImpl useCase = new ConsultarAsignaturasUseCaseImpl(scopePort, queryPort);

    private static AsignaturaProjection asignatura(final UUID idPrograma) {
        return new AsignaturaProjection(UUID.randomUUID(), "A1", "Algebra", 3, UUID.randomUUID(), "Area", UUID.randomUUID(),
                "Componente", UUID.randomUUID(), UUID.randomUUID(), idPrograma, "Sistemas", "S1", true, "SI");
    }

    @Test
    void coordinador_consulta_las_asignaturas_de_su_programa() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarAsignaturasPorPrograma(PROGRAMA)).thenReturn(List.of(asignatura(PROGRAMA)));

        final List<AsignaturaDomain> resultado = useCase.execute(ACTOR);

        assertEquals(1, resultado.size());
        verify(queryPort, never()).consultarAsignaturasPorFacultad(any());
    }

    @Test
    void decano_consulta_las_asignaturas_de_su_facultad() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
        when(queryPort.consultarAsignaturasPorFacultad(FACULTAD))
                .thenReturn(List.of(asignatura(UUID.randomUUID()), asignatura(UUID.randomUUID())));

        final List<AsignaturaDomain> resultado = useCase.execute(ACTOR);

        assertEquals(2, resultado.size());
        verify(queryPort, never()).consultarAsignaturasPorPrograma(any());
    }

    @Test
    void sin_programa_ni_facultad_se_rechaza_sin_consultar() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR));

        verifyNoInteractions(queryPort);
    }
}
