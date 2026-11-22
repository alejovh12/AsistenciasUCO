package co.edu.uco.asistenciasuco.application.features.recesoacademico;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.usecase.impl.ConsultarRecesosPeriodoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.primaryports.RetirarRecesoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.usecase.impl.RetirarRecesoAcademicoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.RecesoPeriodo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** DEC-081: retirar y consultar los dias de receso; la forma se valida aqui, las reglas y el ambito son de la DB. */
class RetirarYConsultarRecesoAcademicoUseCaseImplTest {

    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID RECESO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    private final RecesoAcademicoRepositoryPort repositorio = mock(RecesoAcademicoRepositoryPort.class);
    private final RetirarRecesoAcademicoUseCaseImpl retirar = new RetirarRecesoAcademicoUseCaseImpl(repositorio);
    private final ConsultarRecesosPeriodoUseCaseImpl consultar = new ConsultarRecesosPeriodoUseCaseImpl(repositorio);

    @Test
    void retirar_devuelve_el_mensaje_de_la_db() {
        when(repositorio.retirar(RECESO, USUARIO)).thenReturn("Se retiró el receso académico del 03/12/2026.");

        final var resultado = retirar.execute(new RetirarRecesoAcademicoInputPort.Comando(RECESO, USUARIO));

        assertEquals("Se retiró el receso académico del 03/12/2026.", resultado.mensajeUsuario());
    }

    @Test
    void retirar_un_dia_ya_aplicado_propaga_el_conflicto_rec_010() {
        when(repositorio.retirar(RECESO, USUARIO)).thenThrow(new ConflictException(MensajeUsuarioErrorCode.REC_010));

        assertThrows(ConflictException.class, () -> retirar.execute(new RetirarRecesoAcademicoInputPort.Comando(RECESO, USUARIO)));
    }

    @Test
    void retirar_valida_la_forma_sin_llamar_a_la_db() {
        assertThrows(ValidationException.class, () -> retirar.execute(new RetirarRecesoAcademicoInputPort.Comando(null, USUARIO)));
        assertThrows(ValidationException.class, () -> retirar.execute(new RetirarRecesoAcademicoInputPort.Comando(RECESO, null)));
        assertThrows(NullPointerException.class, () -> retirar.execute(null));
        assertThrows(NullPointerException.class, () -> new RetirarRecesoAcademicoUseCaseImpl(null));
        verifyNoInteractions(repositorio);
    }

    @Test
    void consultar_devuelve_los_dias_tal_como_los_ordena_la_db() {
        final var dias = List.of(new RecesoPeriodo(RECESO, PERIODO, LocalDate.of(2026, 12, 3), "Jornada", 4, true, false,
                LocalDateTime.of(2026, 11, 21, 10, 0)));
        when(repositorio.consultar(PERIODO, USUARIO)).thenReturn(dias);

        assertEquals(List.of(new ConsultarRecesosPeriodoInputPort.RecesoDia(RECESO, LocalDate.of(2026, 12, 3), "Jornada", 4, true, false)),
                consultar.execute(new ConsultarRecesosPeriodoInputPort.Comando(PERIODO, USUARIO)));
        verify(repositorio).consultar(PERIODO, USUARIO);
    }

    @Test
    void consultar_valida_la_forma_sin_llamar_a_la_db() {
        assertThrows(ValidationException.class, () -> consultar.execute(new ConsultarRecesosPeriodoInputPort.Comando(null, USUARIO)));
        assertThrows(ValidationException.class, () -> consultar.execute(new ConsultarRecesosPeriodoInputPort.Comando(PERIODO, null)));
        assertThrows(NullPointerException.class, () -> consultar.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarRecesosPeriodoUseCaseImpl(null));
        verifyNoInteractions(repositorio);
    }
}