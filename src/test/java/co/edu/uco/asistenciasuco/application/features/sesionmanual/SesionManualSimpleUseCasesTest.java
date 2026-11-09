package co.edu.uco.asistenciasuco.application.features.sesionmanual;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.primaryports.ConsultarMotivosCancelacionInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.primaryports.ConsultarMotivosCancelacionInputPort.Motivo;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.usecase.impl.ConsultarMotivosCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.NotificarSesionManualInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.interactor.CrearSesionConAvisoInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.MotivoCancelacion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 / DEC-064: consulta de motivos y decorador de la creacion de sesiones. El aviso se dispara tras crear la sesion y nunca
 * puede revertirla ni cambiar su respuesta; si la creacion falla no hay aviso.
 */
class SesionManualSimpleUseCasesTest {

    private static final UUID COORDINADOR = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOCENTE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void consultar_motivos_traduce_el_catalogo_en_su_orden() {
        final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
        when(repositorio.consultarMotivosCancelacion(COORDINADOR))
                .thenReturn(List.of(new MotivoCancelacion("DUPLICADA", "Sesión duplicada"), new MotivoCancelacion("OTRO", "Otro motivo")));

        final List<Motivo> motivos = new ConsultarMotivosCancelacionUseCaseImpl(repositorio).execute(new Comando(COORDINADOR));

        assertEquals(List.of(new Motivo("DUPLICADA", "Sesión duplicada"), new Motivo("OTRO", "Otro motivo")), motivos);
    }

    @Test
    void consultar_motivos_exige_sus_dependencias() {
        final ConsultarMotivosCancelacionUseCaseImpl useCase = new ConsultarMotivosCancelacionUseCaseImpl(mock(SesionManualRepositoryPort.class));
        assertThrows(NullPointerException.class, () -> new ConsultarMotivosCancelacionUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void el_decorador_crea_la_sesion_y_despues_avisa_con_los_datos_de_la_creacion() {
        final CrearSesionInputPort crear = mock(CrearSesionInputPort.class);
        final NotificarSesionManualInputPort notificar = mock(NotificarSesionManualInputPort.class);
        final CrearSesionDTO dto = dto();

        new CrearSesionConAvisoInteractor(crear, notificar).execute(dto);

        final InOrder orden = inOrder(crear, notificar);
        orden.verify(crear).execute(dto);
        final ArgumentCaptor<NotificarSesionManualInputPort.Comando> comando = ArgumentCaptor.forClass(NotificarSesionManualInputPort.Comando.class);
        orden.verify(notificar).execute(comando.capture());
        assertEquals(new NotificarSesionManualInputPort.Comando(GRUPO, "Sesion extra", dto.getFechaHoraInicio(), dto.getFechaHoraFin(), DOCENTE),
                comando.getValue());
    }

    @Test
    void si_la_creacion_falla_no_avisa_y_el_error_se_propaga() {
        final CrearSesionInputPort crear = mock(CrearSesionInputPort.class);
        final NotificarSesionManualInputPort notificar = mock(NotificarSesionManualInputPort.class);
        doThrow(new IllegalStateException("conflicto")).when(crear).execute(any());

        assertThrows(IllegalStateException.class, () -> new CrearSesionConAvisoInteractor(crear, notificar).execute(dto()));
        verify(notificar, never()).execute(any());
    }

    @Test
    void si_el_aviso_falla_la_creacion_no_se_revierte_ni_se_propaga_el_error() {
        final CrearSesionInputPort crear = mock(CrearSesionInputPort.class);
        final NotificarSesionManualInputPort notificar = mock(NotificarSesionManualInputPort.class);
        when(notificar.execute(any())).thenThrow(new IllegalStateException("smtp"));

        new CrearSesionConAvisoInteractor(crear, notificar).execute(dto());

        verify(crear).execute(any());
    }

    @Test
    void el_decorador_exige_sus_dependencias() {
        final CrearSesionInputPort crear = mock(CrearSesionInputPort.class);
        final NotificarSesionManualInputPort notificar = mock(NotificarSesionManualInputPort.class);
        assertThrows(NullPointerException.class, () -> new CrearSesionConAvisoInteractor(null, notificar));
        assertThrows(NullPointerException.class, () -> new CrearSesionConAvisoInteractor(crear, null));
    }

    private static CrearSesionDTO dto() {
        return new CrearSesionDTO(GRUPO, "Sesion extra", LocalDateTime.of(2026, 11, 10, 13, 0), LocalDateTime.of(2026, 11, 10, 15, 0), DOCENTE);
    }
}
