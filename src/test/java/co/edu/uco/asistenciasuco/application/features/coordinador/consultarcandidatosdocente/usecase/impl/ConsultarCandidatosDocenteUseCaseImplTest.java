package co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteCandidatoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteCandidatoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU091: candidatos (datos minimos, D4) de la misma institucion para el picker. */
class ConsultarCandidatosDocenteUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DocenteProgramaQueryPort queryPort = mock(DocenteProgramaQueryPort.class);
    private final ConsultarCandidatosDocenteUseCaseImpl useCase = new ConsultarCandidatosDocenteUseCaseImpl(scopePort, queryPort);

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarCandidatosDocenteUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarCandidatosDocenteUseCaseImpl(scopePort, null));
    }

    @Test
    void sin_programa_lanza_forbidden() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR, "ros"));

        verifyNoInteractions(queryPort);
    }

    @Test
    void T_091_18_consulta_por_programa_del_ambito_y_mapea_datos_minimos() {
        final UUID docente = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarCandidatos(ACTOR, PROGRAMA, "ros"))
                .thenReturn(List.of(new DocenteCandidatoProjection(docente, usuario, "CC", 1017555444, "Maria Rostagno")));

        final List<DocenteCandidatoDomain> resultado = useCase.execute(ACTOR, "ros");

        assertEquals(1, resultado.size());
        assertEquals(docente, resultado.get(0).idDocente());
        assertEquals(usuario, resultado.get(0).idUsuario());
        assertEquals("CC", resultado.get(0).codigoTipoIdentificacion());
        assertEquals(1017555444, resultado.get(0).numeroIdentificacion());
        assertEquals("Maria Rostagno", resultado.get(0).nombreCompleto());
    }
}
