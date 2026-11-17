package co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.interactor.ConsultarResumenMateriasEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.domain.ResumenMateriasEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.impl.ConsultarResumenMateriasEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenMateriasEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenMateriasEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W2B: el resumen del tablero llega tal como lo calcula la base; ni el caso de uso ni el interactor suman o clasifican. */
class ConsultarResumenMateriasEstudianteFeatureTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final ResumenMateriasEstudianteQueryPort queryPort = mock(ResumenMateriasEstudianteQueryPort.class);
    private final ConsultarResumenMateriasEstudianteUseCaseImpl useCase = new ConsultarResumenMateriasEstudianteUseCaseImpl(scopePort, queryPort);
    private final ConsultarResumenMateriasEstudianteInteractor interactor = new ConsultarResumenMateriasEstudianteInteractor(useCase);
    private final UUID actor = UUID.randomUUID();

    @Test
    void transporta_las_cuatro_cifras_de_la_base_con_el_usuario_ejecutor() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarResumen(actor)).thenReturn(new ResumenMateriasEstudianteProjection(4, 2, new BigDecimal("6.25"), 11));

        final ResumenMateriasEstudianteDTO dto = interactor.execute(actor);

        assertEquals(new ResumenMateriasEstudianteDTO(4, 2, new BigDecimal("6.25"), 11), dto);
        verify(queryPort).consultarResumen(actor);
    }

    @Test
    void sin_umbrales_el_riesgo_sigue_siendo_nulo_y_nunca_se_convierte_en_cero() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarResumen(actor)).thenReturn(new ResumenMateriasEstudianteProjection(2, null, BigDecimal.ZERO, 6));

        final ResumenMateriasEstudianteDomain dominio = useCase.execute(actor);

        assertNull(dominio.materiasEnRiesgo());
        assertEquals(2, dominio.totalMaterias());
    }

    @Test
    void sin_materias_son_ceros() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarResumen(actor)).thenReturn(new ResumenMateriasEstudianteProjection(0, 0, null, 0));

        final ResumenMateriasEstudianteDTO dto = interactor.execute(actor);

        assertEquals(0, dto.totalMaterias());
        assertEquals(0, dto.materiasEnRiesgo());
        assertEquals(BigDecimal.ZERO, dto.totalHorasFalta());
        assertEquals(0, dto.totalCreditos());
    }

    @Test
    void un_usuario_que_no_es_estudiante_es_403_y_no_consulta_la_base() {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
        verify(queryPort, never()).consultarResumen(actor);
    }

    @Test
    void las_dependencias_son_obligatorias() {
        assertThrows(NullPointerException.class, () -> new ConsultarResumenMateriasEstudianteUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenMateriasEstudianteUseCaseImpl(scopePort, null));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenMateriasEstudianteInteractor(null));
    }
}
