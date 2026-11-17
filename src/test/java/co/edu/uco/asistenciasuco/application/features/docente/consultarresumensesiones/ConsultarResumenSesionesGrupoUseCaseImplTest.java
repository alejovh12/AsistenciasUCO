package co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.primaryports.ConsultarResumenSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.result.ResumenSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.usecase.impl.ConsultarResumenSesionesGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenSesionesGrupoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W3M (puntos 2 y 3): resumen de sesiones del grupo resuelto en la base (estado temporal, conteos e indicadores). */
class ConsultarResumenSesionesGrupoUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID EJECUTADA = UUID.randomUUID();
    private static final UUID EN_CURSO = UUID.randomUUID();
    private static final UUID PROXIMA = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ResumenSesionesGrupoQueryPort resumen = mock(ResumenSesionesGrupoQueryPort.class);
    private final ConsultarResumenSesionesGrupoUseCaseImpl useCase = new ConsultarResumenSesionesGrupoUseCaseImpl(scope, resumen);

    @Test
    void el_titular_obtiene_el_resumen_del_grupo_y_de_cada_sesion_tal_como_lo_entrega_la_base() {
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        when(resumen.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO)).thenReturn(new ResumenSesionesGrupoQueryPort.ResumenGrupo(
                3, 1, 0, 1, EN_CURSO, PROXIMA, 30, 4, new BigDecimal("87.50"), new BigDecimal("15.00"), new BigDecimal("20.00"), List.of(
                new ResumenSesionesGrupoQueryPort.SesionResumen(EJECUTADA, 1, "S1", Instant.parse("2026-11-01T13:00:00Z"),
                        Instant.parse("2026-11-01T15:00:00Z"), true, "EJECUTADA", null, false, 30, 25, 2, 1, 1, 1),
                new ResumenSesionesGrupoQueryPort.SesionResumen(EN_CURSO, 2, "S2", Instant.parse("2026-11-16T13:00:00Z"),
                        Instant.parse("2026-11-16T15:00:00Z"), true, "EN_CURSO", "APROBADA", false, 30, 0, 0, 0, 0, 30))));

        final ResumenSesionesGrupoDTO dto = useCase.execute(new ConsultarResumenSesionesGrupoInputPort.Consulta(GRUPO, USUARIO));

        assertEquals(3, dto.sesionesProgramadas());
        assertEquals(1, dto.sesionesEjecutadas());
        assertEquals(1, dto.sesionesEnCurso());
        assertEquals(EN_CURSO, dto.idSesionEnCurso());
        assertEquals(PROXIMA, dto.idSesionProxima());
        assertEquals(30, dto.estudiantesActivos());
        assertEquals(4, dto.estudiantesEnRiesgo());
        assertEquals(new BigDecimal("87.50"), dto.promedioAsistencia());
        assertEquals(new BigDecimal("20.00"), dto.umbralPerdida());
        assertEquals(new ResumenSesionesGrupoDTO.Sesion(EJECUTADA, 1, "S1", Instant.parse("2026-11-01T13:00:00Z"),
                Instant.parse("2026-11-01T15:00:00Z"), true, "EJECUTADA", null, false, 30, 25, 2, 1, 1, 1), dto.sesiones().getFirst());
        assertEquals("APROBADA", dto.sesiones().get(1).estadoSolicitud());
    }

    @Test
    void un_grupo_sin_sesiones_ni_horas_dictadas_no_inventa_promedio() {
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        when(resumen.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO))
                .thenReturn(new ResumenSesionesGrupoQueryPort.ResumenGrupo(0, 0, 0, 0, null, null, 2, 0, null, null, null, null));

        final ResumenSesionesGrupoDTO dto = useCase.execute(new ConsultarResumenSesionesGrupoInputPort.Consulta(GRUPO, USUARIO));

        assertNull(dto.promedioAsistencia());
        assertNull(dto.idSesionEnCurso());
        assertEquals(List.of(), dto.sesiones());
    }

    @Test
    void un_docente_no_titular_recibe_403_sin_consultar_la_base() {
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarResumenSesionesGrupoInputPort.Consulta(GRUPO, USUARIO)));

        verify(resumen, never()).consultarResumenSesionesGrupoDocente(any(), any());
    }

    @Test
    void sin_grupo_o_sin_ejecutor_es_validacion_y_los_puertos_son_obligatorios() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarResumenSesionesGrupoInputPort.Consulta(null, USUARIO)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarResumenSesionesGrupoInputPort.Consulta(GRUPO, null)));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenSesionesGrupoUseCaseImpl(null, resumen));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenSesionesGrupoUseCaseImpl(scope, null));
    }
}
