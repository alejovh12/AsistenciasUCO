package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ComparativaFacultadDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ConsultarComparativaFacultadesDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.impl.ConsultarComparativaFacultadesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaFacultadProjection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarComparativaFacultadesUseCaseImplTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID F1 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID F2 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID F3 = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final AdminIndicadoresQueryPort queryPort = mock(AdminIndicadoresQueryPort.class);
    private final ConsultarComparativaFacultadesUseCaseImpl useCase = new ConsultarComparativaFacultadesUseCaseImpl(queryPort);

    @Test
    void T_BE_01_conserva_filas_orden_y_cifras_del_puerto() {
        when(queryPort.consultarComparativa(ACTOR, PERIODO)).thenReturn(List.of(
                new ComparativaFacultadProjection(F1, "Ingenieria", true, 3, 6, 1, 3, new BigDecimal("50.00"), 35, 6, new BigDecimal("17.14")),
                new ComparativaFacultadProjection(F2, "Derecho", true, 1, 0, 0, 0, null, 0, 0, null),
                new ComparativaFacultadProjection(F3, "Cerrada", false, 0, 0, 0, 0, null, 0, 0, null)));

        final List<ComparativaFacultadDomain> r = useCase.execute(new ConsultarComparativaFacultadesDomain(ACTOR, PERIODO));

        assertEquals(List.of(F1, F2, F3), r.stream().map(ComparativaFacultadDomain::idFacultad).toList());
        assertEquals("Ingenieria", r.get(0).nombreFacultad());
        assertTrue(r.get(0).estaActivaFacultad());
        assertEquals(3, r.get(0).totalProgramas());
        assertEquals(6, r.get(0).totalEstudiantes());
        assertEquals(1, r.get(0).estudiantesRiesgo());
        assertEquals(3, r.get(0).estudiantesPerdida());
        assertEquals(new BigDecimal("50.00"), r.get(0).tasaRetencion());
        assertEquals(35, r.get(0).sesionesEjecutadas());
        assertEquals(6, r.get(0).faltasSinJustaCausa());
        assertEquals(new BigDecimal("17.14"), r.get(0).porcentajeAusentismo());
        assertFalse(r.get(2).estaActivaFacultad());
    }

    @Test
    void T_BE_03_una_facultad_sin_datos_conserva_nulos_y_no_se_oculta() {
        when(queryPort.consultarComparativa(ACTOR, null)).thenReturn(List.of(
                new ComparativaFacultadProjection(F2, "Derecho", true, 1, 0, 0, 0, null, 0, 0, null)));

        final List<ComparativaFacultadDomain> r = useCase.execute(new ConsultarComparativaFacultadesDomain(ACTOR, null));

        assertEquals(1, r.size());
        assertNull(r.get(0).tasaRetencion());
        assertNull(r.get(0).porcentajeAusentismo());
    }

    @Test
    void sin_facultades_devuelve_lista_vacia() {
        when(queryPort.consultarComparativa(ACTOR, null)).thenReturn(List.of());

        assertTrue(useCase.execute(new ConsultarComparativaFacultadesDomain(ACTOR, null)).isEmpty());
    }

    @Test
    void el_dominio_es_obligatorio() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
    }

    @Test
    void el_puerto_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ConsultarComparativaFacultadesUseCaseImpl(null));
    }
}
