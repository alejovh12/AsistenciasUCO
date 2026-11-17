package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.OpcionesConsolidadoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.interactor.ConsultasTransversalesInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.OpcionFiltroProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.OpcionesConsolidadoProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CIERRE-W2A (punto 3): opciones de los filtros del consolidado, caso de uso e interactor. */
class ConsultasOpcionesConsolidadoW2ATest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();

    private final ConsultasTransversalesQueryPort port = mock(ConsultasTransversalesQueryPort.class);
    private final ConsultasTransversalesUseCaseImpl useCase = new ConsultasTransversalesUseCaseImpl(port);

    @Test
    void el_caso_de_uso_delega_el_usuario_y_el_periodo_en_el_puerto() {
        final var opciones = new OpcionesConsolidadoProjection(
                List.of(new OpcionFiltroProjection(ASIGNATURA, "Calculo")), List.of(new OpcionFiltroProjection(DOCENTE, "Ana")));
        when(port.consultarOpcionesConsolidado(USUARIO, PERIODO)).thenReturn(opciones);

        assertSame(opciones, useCase.consultarOpcionesConsolidado(USUARIO, PERIODO));
        verify(port).consultarOpcionesConsolidado(USUARIO, PERIODO);
    }

    @Test
    void sin_periodo_pide_todos_los_periodos() {
        final var vacias = new OpcionesConsolidadoProjection(List.of(), List.of());
        when(port.consultarOpcionesConsolidado(USUARIO, null)).thenReturn(vacias);

        assertSame(vacias, useCase.consultarOpcionesConsolidado(USUARIO, null));
    }

    @Test
    void sin_usuario_autenticado_es_403_y_no_consulta() {
        assertThrows(ForbiddenException.class, () -> useCase.consultarOpcionesConsolidado(null, PERIODO));
        verifyNoInteractions(port);
    }

    @Test
    void el_interactor_mapea_asignaturas_y_docentes_al_dto() {
        final var useCaseMock = mock(co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase.class);
        when(useCaseMock.consultarOpcionesConsolidado(USUARIO, PERIODO)).thenReturn(new OpcionesConsolidadoProjection(
                List.of(new OpcionFiltroProjection(ASIGNATURA, "Calculo")), List.of(new OpcionFiltroProjection(DOCENTE, "Ana"))));

        final OpcionesConsolidadoDTO dto = new ConsultasTransversalesInteractor(useCaseMock).consultarOpcionesConsolidado(USUARIO, PERIODO);

        assertEquals(ASIGNATURA, dto.asignaturas().getFirst().id());
        assertEquals("Calculo", dto.asignaturas().getFirst().nombre());
        assertEquals(DOCENTE, dto.docentes().getFirst().id());
        assertEquals("Ana", dto.docentes().getFirst().nombre());
    }

    @Test
    void el_interactor_entrega_listas_vacias_cuando_no_hay_opciones() {
        final var useCaseMock = mock(co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase.class);
        when(useCaseMock.consultarOpcionesConsolidado(USUARIO, null)).thenReturn(new OpcionesConsolidadoProjection(List.of(), List.of()));

        final OpcionesConsolidadoDTO dto = new ConsultasTransversalesInteractor(useCaseMock).consultarOpcionesConsolidado(USUARIO, null);

        assertTrue(dto.asignaturas().isEmpty());
        assertTrue(dto.docentes().isEmpty());
    }
}
