package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.domain.ConfiguracionReclamosDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConfiguracionReclamosQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ConfiguracionReclamosProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 D.2 (CONTRACT D 3.3): GET /api/v1/configuracion/reclamos. La configuracion sale de la base de datos
 * (usp_consultar_configuracion_reclamos valida el rol ESTUDIANTE o DOCENTE); Java solo la transporta y separa los tipos permitidos.
 */
class ConsultarConfiguracionReclamosUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final ConfiguracionReclamosQueryPort queryPort = mock(ConfiguracionReclamosQueryPort.class);
    private final ConsultarConfiguracionReclamosUseCaseImpl useCase = new ConsultarConfiguracionReclamosUseCaseImpl(queryPort);

    @Test
    void transporta_la_configuracion_y_separa_los_tipos_permitidos_en_orden() {
        when(queryPort.consultar(ACTOR)).thenReturn(
                new ConfiguracionReclamosProjection(3, 5_242_880L, "application/pdf, image/png ,image/jpeg", 10, 5));

        final ConfiguracionReclamosDomain domain = useCase.execute(ACTOR);

        assertEquals(new ConfiguracionReclamosDomain(3, 5_242_880L, List.of("application/pdf", "image/png", "image/jpeg"), 10, 5), domain);
    }

    @Test
    void ignora_elementos_vacios_y_tipos_ausentes() {
        when(queryPort.consultar(ACTOR)).thenReturn(new ConfiguracionReclamosProjection(1, 1L, " ,application/pdf,, ", 1, 0));
        assertEquals(List.of("application/pdf"), useCase.execute(ACTOR).tiposPermitidos());

        when(queryPort.consultar(ACTOR)).thenReturn(new ConfiguracionReclamosProjection(1, 1L, "   ", 1, 0));
        assertEquals(List.of(), useCase.execute(ACTOR).tiposPermitidos());

        when(queryPort.consultar(ACTOR)).thenReturn(new ConfiguracionReclamosProjection(1, 1L, null, 1, 0));
        assertEquals(List.of(), useCase.execute(ACTOR).tiposPermitidos());
    }

    @Test
    void la_lista_de_tipos_es_inmutable() {
        when(queryPort.consultar(ACTOR)).thenReturn(new ConfiguracionReclamosProjection(1, 1L, "application/pdf", 1, 0));
        final List<String> tipos = useCase.execute(ACTOR).tiposPermitidos();
        assertThrows(UnsupportedOperationException.class, () -> tipos.add("image/png"));
    }

    @Test
    void sin_usuario_autenticado_falla_antes_de_consultar() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        verify(queryPort, never()).consultar(any());
    }

    @Test
    void una_configuracion_incompleta_es_un_fallo_interno_y_no_se_inventan_valores() {
        when(queryPort.consultar(ACTOR)).thenReturn(null);
        assertThrows(InternalApplicationException.class, () -> useCase.execute(ACTOR));

        for (final ConfiguracionReclamosProjection incompleta : List.of(
                new ConfiguracionReclamosProjection(null, 1L, "application/pdf", 1, 0),
                new ConfiguracionReclamosProjection(3, null, "application/pdf", 1, 0),
                new ConfiguracionReclamosProjection(3, 1L, "application/pdf", null, 0),
                new ConfiguracionReclamosProjection(3, 1L, "application/pdf", 1, null))) {
            when(queryPort.consultar(ACTOR)).thenReturn(incompleta);
            assertThrows(InternalApplicationException.class, () -> useCase.execute(ACTOR));
        }
    }

    @Test
    void exige_el_puerto_de_consulta() {
        assertThrows(NullPointerException.class, () -> new ConsultarConfiguracionReclamosUseCaseImpl(null));
    }
}
