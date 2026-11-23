package co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ActualizarSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ResultadoSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.interactor.ActualizarSemestreEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.ActualizarSemestreEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain.ActualizarSemestreEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain.ResultadoSemestreEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteSemestreCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarSemestreEstudianteCommand;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActualizarSemestreEstudianteUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();

    private final EstudianteSemestreCommandPort port = mock(EstudianteSemestreCommandPort.class);
    private final ActualizarSemestreEstudianteUseCaseImpl useCase = new ActualizarSemestreEstudianteUseCaseImpl(port);

    @Test
    void delega_en_el_puerto_y_devuelve_el_mensaje() {
        final var comando = new ActualizarSemestreEstudianteCommand(ESTUDIANTE, PROGRAMA, 4, ACTOR);
        when(port.actualizarSemestre(comando)).thenReturn("Listo");

        final var resultado = useCase.execute(new ActualizarSemestreEstudianteDomain(ACTOR, ESTUDIANTE, PROGRAMA, 4));

        assertEquals("Listo", resultado.mensajeUsuario());
        verify(port).actualizarSemestre(comando);
    }

    @Test
    void semestre_nulo_limpia_y_se_propaga() {
        when(port.actualizarSemestre(new ActualizarSemestreEstudianteCommand(ESTUDIANTE, PROGRAMA, null, ACTOR))).thenReturn("ok");
        assertEquals("ok", useCase.execute(new ActualizarSemestreEstudianteDomain(ACTOR, ESTUDIANTE, PROGRAMA, null)).mensajeUsuario());
    }

    @Test
    void rechaza_dominio_o_identificadores_nulos_sin_llamar_al_puerto() {
        final var sinEstudiante = new ActualizarSemestreEstudianteDomain(ACTOR, null, PROGRAMA, 1);
        final var sinPrograma = new ActualizarSemestreEstudianteDomain(ACTOR, ESTUDIANTE, null, 1);
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        assertThrows(CrosscuttingException.class, () -> useCase.execute(sinEstudiante));
        assertThrows(CrosscuttingException.class, () -> useCase.execute(sinPrograma));
        verifyNoInteractions(port);
    }

    @Test
    void constructores_rechazan_nulos() {
        assertThrows(NullPointerException.class, () -> new ActualizarSemestreEstudianteUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ActualizarSemestreEstudianteInteractor(null));
    }

    @Test
    void el_interactor_traslada_el_dto_y_devuelve_el_mensaje() {
        final var caso = mock(ActualizarSemestreEstudianteUseCase.class);
        final var domain = new ActualizarSemestreEstudianteDomain(ACTOR, ESTUDIANTE, PROGRAMA, 7);
        when(caso.execute(domain)).thenReturn(new ResultadoSemestreEstudianteDomain("Hecho"));

        final ResultadoSemestreEstudianteDTO dto = new ActualizarSemestreEstudianteInteractor(caso)
                .execute(new ActualizarSemestreEstudianteDTO(ACTOR, ESTUDIANTE, PROGRAMA, 7));

        assertEquals("Hecho", dto.mensajeUsuario());
    }
}
