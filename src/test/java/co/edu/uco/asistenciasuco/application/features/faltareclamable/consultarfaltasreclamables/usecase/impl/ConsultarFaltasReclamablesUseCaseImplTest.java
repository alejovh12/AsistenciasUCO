package co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.FaltaReclamable;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.FilaFaltaReclamable;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.PaginaFaltasReclamables;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ATA D10: caso de uso de las faltas por reclamar del estudiante (paginacion por defecto y mapeo de la fila). */
class ConsultarFaltasReclamablesUseCaseImplTest {

    private static final UUID USUARIO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASIGNATURA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TOMA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant INICIO = Instant.parse("2026-11-17T13:00:00Z");

    private final FaltaReclamableRepositoryPort repositorio = mock(FaltaReclamableRepositoryPort.class);
    private final ConsultarFaltasReclamablesUseCaseImpl caso = new ConsultarFaltasReclamablesUseCaseImpl(repositorio);

    private static FilaFaltaReclamable fila(final int toma) {
        return new FilaFaltaReclamable(GRUPO, "G01", ASIGNATURA, "Calculo", SESION, 4, INICIO, INICIO.plusSeconds(7200), TOMA, toma,
                INICIO.plusSeconds(2400), Instant.parse("2026-11-23T05:00:00Z"));
    }

    @Test
    void sin_pagina_ni_tamano_usa_1_y_20_y_mapea_la_fila_y_el_total() {
        when(repositorio.consultarEstudiante(USUARIO, 1, 20)).thenReturn(new PaginaFaltasReclamables(List.of(fila(1), fila(3)), 7));

        final Resultado resultado = caso.execute(new Comando(USUARIO, null, null));

        assertEquals(1, resultado.pagina());
        assertEquals(20, resultado.tamano());
        assertEquals(7, resultado.total());
        assertEquals(2, resultado.elementos().size());
        assertEquals(new FaltaReclamable(GRUPO, "G01", ASIGNATURA, "Calculo", SESION, 4, INICIO, INICIO.plusSeconds(7200), TOMA, 1,
                INICIO.plusSeconds(2400), Instant.parse("2026-11-23T05:00:00Z")), resultado.elementos().getFirst());
        assertEquals(3, resultado.elementos().get(1).numeroToma());
    }

    @Test
    void respeta_la_pagina_y_el_tamano_pedidos() {
        when(repositorio.consultarEstudiante(USUARIO, 3, 5)).thenReturn(new PaginaFaltasReclamables(List.of(), 0));

        final Resultado resultado = caso.execute(new Comando(USUARIO, 3, 5));

        assertEquals(3, resultado.pagina());
        assertEquals(5, resultado.tamano());
        assertTrue(resultado.elementos().isEmpty());
        verify(repositorio).consultarEstudiante(USUARIO, 3, 5);
    }

    @Test
    void el_resultado_sin_elementos_es_una_lista_vacia() {
        assertTrue(new Resultado(null, 1, 20, 0).elementos().isEmpty());
    }

    @Test
    void valida_sus_dependencias_y_el_comando() {
        assertThrows(NullPointerException.class, () -> new ConsultarFaltasReclamablesUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> caso.execute(null));
    }
}
