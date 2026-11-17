package co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.common.dto.ResumenGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.primaryports.ConsultarResumenGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.usecase.impl.ConsultarResumenGruposDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenGruposDocenteQueryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CIERRE-W3I: totales de los grupos habilitados del docente resueltos en la base; el caso de uso solo orquesta. */
class ConsultarResumenGruposDocenteUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();

    private final ResumenGruposDocenteQueryPort resumen = mock(ResumenGruposDocenteQueryPort.class);
    private final ConsultarResumenGruposDocenteUseCaseImpl useCase = new ConsultarResumenGruposDocenteUseCaseImpl(resumen);

    @Test
    void entrega_los_totales_tal_como_los_calcula_la_base() {
        when(resumen.consultarResumenGruposDocente(USUARIO))
                .thenReturn(new ResumenGruposDocenteQueryPort.ResumenGrupos(2, 4, 50, new BigDecimal("8.00")));

        final ResumenGruposDocenteDTO dto = useCase.execute(new ConsultarResumenGruposDocenteInputPort.Consulta(USUARIO));

        assertEquals(new ResumenGruposDocenteDTO(2, 4, 50, new BigDecimal("8.00")), dto);
    }

    @Test
    void con_cupo_total_cero_la_tasa_de_ocupacion_sigue_siendo_nula() {
        when(resumen.consultarResumenGruposDocente(USUARIO))
                .thenReturn(new ResumenGruposDocenteQueryPort.ResumenGrupos(0, 0, 0, null));

        final ResumenGruposDocenteDTO dto = useCase.execute(new ConsultarResumenGruposDocenteInputPort.Consulta(USUARIO));

        assertEquals(0, dto.gruposHabilitados());
        assertNull(dto.tasaOcupacion());
    }

    @Test
    void sin_ejecutor_o_sin_consulta_rechaza_sin_consultar_la_base() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        final var consulta = new ConsultarResumenGruposDocenteInputPort.Consulta(null);
        assertThrows(ValidationException.class, () -> useCase.execute(consulta));
        verifyNoInteractions(resumen);
    }

    @Test
    void el_constructor_exige_el_puerto() {
        assertThrows(NullPointerException.class, () -> new ConsultarResumenGruposDocenteUseCaseImpl(null));
    }
}
