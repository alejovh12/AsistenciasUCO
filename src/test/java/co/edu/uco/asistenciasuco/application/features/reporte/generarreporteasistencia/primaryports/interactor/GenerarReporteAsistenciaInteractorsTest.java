package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.GenerarReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.GenerarReporteAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.domain.GenerarReporteAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteExcelPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** M-10: interactores del reporte de asistencia (lista y libro de Excel). */
class GenerarReporteAsistenciaInteractorsTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final ReporteAsistenciaEntity ENTIDAD = new ReporteAsistenciaEntity("G01", "Grupo 01", 2, "Sesión 2",
            LocalDateTime.of(2026, 3, 1, 8, 0), LocalDateTime.of(2026, 3, 1, 10, 0), "123", "Ana", "ana@uco.edu.co",
            Boolean.FALSE, "Enfermedad");

    @Test
    void el_interactor_de_lista_convierte_las_entidades_a_dto() {
        final GenerarReporteAsistenciaUseCase useCase = mock(GenerarReporteAsistenciaUseCase.class);
        when(useCase.execute(any(GenerarReporteAsistenciaDomain.class))).thenReturn(List.of(ENTIDAD));

        final var filas = new GenerarReporteAsistenciaInteractor(useCase).execute(new GenerarReporteAsistenciaDTO(GRUPO, ACTOR));

        assertEquals(1, filas.size());
        assertEquals("G01", filas.get(0).codigoGrupo());
        assertEquals(2, filas.get(0).numeroSesion());
        assertEquals("Enfermedad", filas.get(0).razonCausa());
        assertEquals(Boolean.FALSE, filas.get(0).asistio());
    }

    @Test
    void el_interactor_de_excel_entrega_las_filas_al_puerto_y_devuelve_su_libro() {
        final GenerarReporteAsistenciaUseCase useCase = mock(GenerarReporteAsistenciaUseCase.class);
        final ReporteExcelPort puerto = mock(ReporteExcelPort.class);
        final byte[] libro = {9, 8};
        when(useCase.execute(any(GenerarReporteAsistenciaDomain.class))).thenReturn(List.of(ENTIDAD));
        when(puerto.generarReporteAsistencia(any())).thenReturn(libro);

        final byte[] resultado = new GenerarReporteAsistenciaExcelInteractor(useCase, puerto)
                .execute(new GenerarReporteAsistenciaDTO(GRUPO, ACTOR));

        assertArrayEquals(libro, resultado);
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<ReporteAsistenciaRow>> filas = ArgumentCaptor.forClass(List.class);
        verify(puerto).generarReporteAsistencia(filas.capture());
        assertEquals("Ana", filas.getValue().get(0).nombreEstudiante());
        assertEquals("ana@uco.edu.co", filas.getValue().get(0).correoEstudiante());
    }

    @Test
    void los_interactores_exigen_sus_dependencias() {
        final GenerarReporteAsistenciaUseCase useCase = mock(GenerarReporteAsistenciaUseCase.class);
        assertThrows(NullPointerException.class, () -> new GenerarReporteAsistenciaInteractor(null));
        assertThrows(NullPointerException.class, () -> new GenerarReporteAsistenciaExcelInteractor(null, mock(ReporteExcelPort.class)));
        assertThrows(NullPointerException.class, () -> new GenerarReporteAsistenciaExcelInteractor(useCase, null));
    }
}
