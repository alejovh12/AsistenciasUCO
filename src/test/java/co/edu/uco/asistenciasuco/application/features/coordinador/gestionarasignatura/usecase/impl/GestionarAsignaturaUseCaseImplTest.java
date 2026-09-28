package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GestionarAsignaturaUseCaseImplTest {

    private final AsignaturaCommandPort commandPort = mock(AsignaturaCommandPort.class);
    private final GestionarAsignaturaUseCaseImpl useCase = new GestionarAsignaturaUseCaseImpl(commandPort);

    @Test
    void crear_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.crear(null));
    }

    @Test
    void crear_invoca_command_port_con_datos_del_dominio() {
        final UUID plan = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final AsignaturaDomain domain = new AsignaturaDomain(
                null, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", usuarioEjecutor
        );

        useCase.crear(domain);

        verify(commandPort).crearAsignatura(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("COD1"),
                org.mockito.ArgumentMatchers.eq("Calculo"), org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(plan), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("Area"), org.mockito.ArgumentMatchers.eq("Componente"),
                org.mockito.ArgumentMatchers.eq(usuarioEjecutor));
    }

    @Test
    void actualizar_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.actualizar(null));
    }

    @Test
    void actualizar_invoca_command_port_con_id_de_asignatura() {
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        final AsignaturaDomain domain = new AsignaturaDomain(id, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", null);

        useCase.actualizar(domain);

        final ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).actualizarAsignatura(idCaptor.capture(), org.mockito.ArgumentMatchers.eq("COD1"),
                org.mockito.ArgumentMatchers.eq("Calculo"), org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(plan), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("Area"), org.mockito.ArgumentMatchers.eq("Componente"));
        assertEquals(id, idCaptor.getValue());
    }

    @Test
    void toggleEstado_rechaza_id_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.toggleEstado(null));
    }

    @Test
    void toggleEstado_invoca_command_port() {
        final UUID id = UUID.randomUUID();

        useCase.toggleEstado(id);

        verify(commandPort).toggleEstadoAsignatura(id);
    }
}
