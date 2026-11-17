package co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.primaryports.ConsultarMatrizAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.common.dto.MatrizAsistenciaGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.usecase.impl.ConsultarMatrizAsistenciaGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatrizAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenSesionesGrupoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W3M (punto 1): matriz de asistencia del docente titular, paginada y con busqueda, resuelta por el SP. */
class ConsultarMatrizAsistenciaGrupoUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();
    private static final UUID MATRICULA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final MatrizAsistenciaQueryPort matriz = mock(MatrizAsistenciaQueryPort.class);
    private final ResumenSesionesGrupoQueryPort sesiones = mock(ResumenSesionesGrupoQueryPort.class);
    private final ConsultarMatrizAsistenciaGrupoUseCaseImpl useCase = new ConsultarMatrizAsistenciaGrupoUseCaseImpl(scope, matriz, sesiones);

    private static ResumenSesionesGrupoQueryPort.ResumenGrupo resumenConUnaSesion() {
        return new ResumenSesionesGrupoQueryPort.ResumenGrupo(1, 1, 0, 0, null, null, 1, 0, null, null, null, List.of(
                new ResumenSesionesGrupoQueryPort.SesionResumen(SESION, 1, "Sesion 1", Instant.parse("2026-11-10T13:00:00Z"),
                        Instant.parse("2026-11-10T15:00:00Z"), true, "EJECUTADA", null, false, 1, 1, 0, 0, 0, 0)));
    }

    @Test
    void el_titular_obtiene_la_pagina_con_celdas_y_el_encabezado_de_sesiones_de_la_base() {
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        when(matriz.consultarMatrizGrupoDocente(GRUPO, 2, 10, "ana", USUARIO)).thenReturn(new PaginaResultado<>(List.of(
                new MatrizAsistenciaQueryPort.FilaMatriz(MATRICULA, ESTUDIANTE, "Ana Perez", "100", "A", List.of(
                        new MatrizAsistenciaQueryPort.CeldaMatriz(SESION, "PARCIAL", 60, 120)))), 2, 10, 11));
        when(sesiones.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO)).thenReturn(resumenConUnaSesion());

        final MatrizAsistenciaGrupoDTO resultado = useCase.execute(
                new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(GRUPO, 2, 10, "  ana  ", USUARIO));

        assertEquals(11, resultado.totalElementos());
        assertEquals(2, resultado.numeroPagina());
        assertEquals(10, resultado.tamanoPagina());
        assertEquals(List.of(new MatrizAsistenciaGrupoDTO.Sesion(SESION, 1, "Sesion 1", "EJECUTADA", Instant.parse("2026-11-10T13:00:00Z"),
                Instant.parse("2026-11-10T15:00:00Z"), true)), resultado.sesiones());
        assertEquals(List.of(new MatrizAsistenciaGrupoDTO.Fila(MATRICULA, ESTUDIANTE, "Ana Perez", "100", "A", List.of(
                new MatrizAsistenciaGrupoDTO.Celda(SESION, "PARCIAL", 60, 120)))), resultado.filas());
    }

    @Test
    void sin_pagina_ni_busqueda_usa_los_valores_por_defecto_y_una_busqueda_en_blanco_es_sin_filtro() {
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        when(matriz.consultarMatrizGrupoDocente(GRUPO, 1, 50, null, USUARIO)).thenReturn(new PaginaResultado<>(List.of(), 1, 50, 0));
        when(sesiones.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO))
                .thenReturn(new ResumenSesionesGrupoQueryPort.ResumenGrupo(0, 0, 0, 0, null, null, 0, 0, null, null, null, List.of()));

        final MatrizAsistenciaGrupoDTO sinBusqueda = useCase.execute(new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(GRUPO, null, null, null, USUARIO));
        final MatrizAsistenciaGrupoDTO enBlanco = useCase.execute(new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(GRUPO, null, null, "   ", USUARIO));

        assertEquals(0, sinBusqueda.totalElementos());
        assertEquals(List.of(), sinBusqueda.sesiones());
        assertEquals(List.of(), enBlanco.filas());
        verify(matriz, org.mockito.Mockito.times(2)).consultarMatrizGrupoDocente(GRUPO, 1, 50, null, USUARIO);
    }

    @Test
    void un_docente_no_titular_recibe_403_sin_consultar_la_base() {
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(
                new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(GRUPO, 1, 10, null, USUARIO)));

        verify(matriz, never()).consultarMatrizGrupoDocente(any(), anyInt(), anyInt(), anyString(), any());
        verify(sesiones, never()).consultarResumenSesionesGrupoDocente(any(), any());
    }

    @Test
    void sin_grupo_o_sin_ejecutor_es_validacion_y_los_puertos_son_obligatorios() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(null, 1, 10, null, USUARIO)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(GRUPO, 1, 10, null, null)));
        assertThrows(NullPointerException.class, () -> new ConsultarMatrizAsistenciaGrupoUseCaseImpl(null, matriz, sesiones));
        assertThrows(NullPointerException.class, () -> new ConsultarMatrizAsistenciaGrupoUseCaseImpl(scope, null, sesiones));
        assertThrows(NullPointerException.class, () -> new ConsultarMatrizAsistenciaGrupoUseCaseImpl(scope, matriz, null));
    }
}
