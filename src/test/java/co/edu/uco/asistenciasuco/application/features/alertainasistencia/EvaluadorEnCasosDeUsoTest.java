package co.edu.uco.asistenciasuco.application.features.alertainasistencia;

import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl.EjecutarCierreMasivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.usecase.impl.CerrarTomasVencidasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CULM-001 C.1 (SUPUESTO-C-04): el cierre automatico evalua cada grupo una vez, solo tras cerrar tomas; el evaluador es obligatorio. */
class EvaluadorEnCasosDeUsoTest {

    @Test
    void cerrar_tomas_vencidas_evalua_cada_grupo_una_sola_vez_y_nada_si_no_cerro() {
        final TomaAsistenciaRepositoryPort repositorio = mock(TomaAsistenciaRepositoryPort.class);
        final EvaluadorAlertasInasistencia evaluador = mock(EvaluadorAlertasInasistencia.class);
        final TomaAsistenciaEventos eventos = new TomaAsistenciaEventos(mock(RealtimePublisherPort.class));
        final UUID grupo = UUID.randomUUID();
        when(repositorio.cerrarVencidas()).thenReturn(List.of(
                new TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente(UUID.randomUUID(), UUID.randomUUID(), grupo, 1),
                new TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente(UUID.randomUUID(), UUID.randomUUID(), grupo, 0)));
        when(repositorio.consultarToma(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        new CerrarTomasVencidasUseCaseImpl(repositorio, eventos, evaluador).execute();
        verify(evaluador, times(1)).evaluarYAvisar(grupo, null);

        final EvaluadorAlertasInasistencia ninguno = mock(EvaluadorAlertasInasistencia.class);
        when(repositorio.cerrarVencidas()).thenReturn(List.of());
        new CerrarTomasVencidasUseCaseImpl(repositorio, eventos, ninguno).execute();
        verifyNoInteractions(ninguno);
        assertThrows(NullPointerException.class, () -> new CerrarTomasVencidasUseCaseImpl(repositorio, eventos, null));
        assertThrows(NullPointerException.class, () -> new EjecutarCierreMasivoUseCaseImpl(null, null, null, null));
    }
}
