package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.ConsultarFichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.FichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.impl.ConsultarFichaEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static co.edu.uco.asistenciasuco.application.features.coordinador.kpis.KpisTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarFichaEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CoordinadorIndicadoresQueryPort queryPort = mock(CoordinadorIndicadoresQueryPort.class);
    private final ConsultarFichaEstudianteUseCaseImpl useCase = new ConsultarFichaEstudianteUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void programaDelCoordinador() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarFicha(ACTOR, ESTUDIANTE)).thenReturn(ficha());
        when(queryPort.consultarResumenAsistencia(ACTOR, ESTUDIANTE, PERIODO)).thenReturn(List.of(resumen()));
    }

    @Test
    void T_FIC_01_compone_cabecera_y_resumen_por_asignatura_con_las_cifras_del_puerto() {
        final FichaEstudianteDomain result = useCase.execute(new ConsultarFichaEstudianteDomain(ACTOR, ESTUDIANTE, PERIODO));

        assertEquals(ESTUDIANTE, result.idEstudiante());
        assertEquals("Ana Maria Perez Gomez", result.nombreCompleto());
        assertEquals("ana@uco.edu.co", result.correo());
        assertEquals(1017000001, result.numeroIdentificacion());
        assertEquals("Ingenieria de Sistemas", result.nombrePrograma());
        assertEquals(1, result.resumenPorAsignatura().size());
        assertEquals(2, result.resumenPorAsignatura().getFirst().faltasSinJustaCausa());
        assertEquals(new BigDecimal("33.33"), result.resumenPorAsignatura().getFirst().porcentajeInasistencia());
        assertEquals("PERDIDA", result.resumenPorAsignatura().getFirst().codigoPermanencia());
    }

    @Test
    void consulta_el_resumen_con_el_periodo_indicado_o_nulo() {
        when(queryPort.consultarResumenAsistencia(ACTOR, ESTUDIANTE, null)).thenReturn(List.of());

        final FichaEstudianteDomain result = useCase.execute(new ConsultarFichaEstudianteDomain(ACTOR, ESTUDIANTE, null));

        verify(queryPort).consultarResumenAsistencia(ACTOR, ESTUDIANTE, null);
        assertEquals(0, result.resumenPorAsignatura().size());
    }

    @Test
    void T_BE_02_valida_la_forma_antes_de_llamar_al_puerto() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarFichaEstudianteDomain(ACTOR, null, null)));
        verify(queryPort, never()).consultarFicha(ACTOR, null);
    }

    @Test
    void T_BE_01_sin_programa_de_coordinador_responde_forbidden() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarFichaEstudianteDomain(ACTOR, ESTUDIANTE, null)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarFichaEstudianteUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarFichaEstudianteUseCaseImpl(scopePort, null));
    }
}
