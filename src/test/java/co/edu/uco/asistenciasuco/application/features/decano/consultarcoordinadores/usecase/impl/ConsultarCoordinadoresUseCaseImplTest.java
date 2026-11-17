package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorFacultadProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W1B: el caso de uso pasa el usuario ejecutor y los filtros al SP; el ambito lo valida el SP. */
class ConsultarCoordinadoresUseCaseImplTest {

    private final CoordinadorQueryPort coordinadorQueryPort = mock(CoordinadorQueryPort.class);
    private final ConsultarCoordinadoresUseCaseImpl useCase = new ConsultarCoordinadoresUseCaseImpl(coordinadorQueryPort);

    private static CoordinadorFacultadProjection fila(final UUID id, final UUID programa) {
        return new CoordinadorFacultadProjection(id, UUID.randomUUID(), "123456", "Ana Maria Perez Gomez", programa,
                programa == null ? null : "Ingenieria de Sistemas", true, "Ana", "Maria", "Perez", "Gomez",
                "ana@uco.edu.co", true, "Facultad de Ingenieria", programa == null ? 0 : 4, programa == null ? 0 : 6);
    }

    @Test
    void execute_pasa_ejecutor_filtros_y_pagina_al_puerto_y_conserva_el_total() {
        final UUID actor = UUID.randomUUID();
        final UUID coordinador = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(coordinadorQueryPort.consultarCoordinadoresFacultad(actor, "ana", true, false, 3, 5))
                .thenReturn(new CoordinadorQueryPort.Pagina<>(List.of(fila(coordinador, programa)), 11));

        final PaginaResultado<CoordinadorDomain> resultado = useCase.execute(actor, "ana", true, false, 3, 5);

        verify(coordinadorQueryPort).consultarCoordinadoresFacultad(actor, "ana", true, false, 3, 5);
        assertEquals(3, resultado.numeroPagina());
        assertEquals(5, resultado.tamanoPagina());
        assertEquals(11, resultado.totalElementos());
        final CoordinadorDomain unico = resultado.elementos().getFirst();
        assertEquals(coordinador, unico.id());
        assertEquals(programa, unico.idPrograma());
        assertEquals("Maria", unico.segundoNombre());
        assertEquals("Gomez", unico.segundoApellido());
        assertEquals("ana@uco.edu.co", unico.correo());
        assertEquals("Facultad de Ingenieria", unico.nombreFacultad());
        assertEquals(4, unico.totalDocentes());
        assertEquals(6, unico.totalGrupos());
    }

    @Test
    void execute_sin_filas_devuelve_pagina_vacia_y_sin_programa_no_fabrica_conteos() {
        final UUID actor = UUID.randomUUID();
        when(coordinadorQueryPort.consultarCoordinadoresFacultad(actor, null, null, true, 1, 10))
                .thenReturn(new CoordinadorQueryPort.Pagina<>(List.of(fila(UUID.randomUUID(), null)), 1));
        when(coordinadorQueryPort.consultarCoordinadoresFacultad(actor, "nadie", null, null, 1, 10))
                .thenReturn(new CoordinadorQueryPort.Pagina<>(List.of(), 0));

        final CoordinadorDomain sinPrograma = useCase.execute(actor, null, null, true, 1, 10).elementos().getFirst();
        assertNull(sinPrograma.idPrograma());
        assertEquals(0, sinPrograma.totalDocentes());
        assertEquals(0, sinPrograma.totalGrupos());

        final PaginaResultado<CoordinadorDomain> vacia = useCase.execute(actor, "nadie", null, null, 1, 10);
        assertEquals(0, vacia.elementos().size());
        assertEquals(0, vacia.totalElementos());
    }

    @Test
    void execute_exige_usuario_ejecutor_y_puerto() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null, null, null, null, 1, 10));
        assertThrows(NullPointerException.class, () -> new ConsultarCoordinadoresUseCaseImpl(null));
    }
}
