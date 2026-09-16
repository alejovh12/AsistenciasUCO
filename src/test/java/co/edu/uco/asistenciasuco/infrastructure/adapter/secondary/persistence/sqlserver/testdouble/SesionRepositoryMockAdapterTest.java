package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesionRepositoryMockAdapterTest {

    private static final UUID SESION_CERRADA = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private final SesionRepositoryMockAdapter adapter = new SesionRepositoryMockAdapter();

    @Test
    void metodos_de_comando_rechazan_dto_nulo() {
        assertThrows(CrosscuttingException.class, () -> adapter.crearSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.actualizarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.consultarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.cerrarSesion(null));
        assertThrows(CrosscuttingException.class, () -> adapter.generarSesionesGrupo(null));
    }

    @Test
    void metodos_de_comando_aceptan_dto_valido_sin_lanzar() {
        adapter.crearSesion(new CrearSesionRepositoryDTO(
                UUID.randomUUID(), "Tema", "Descripcion", LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                "Aula 1", "TEORICA", UUID.randomUUID(), UUID.randomUUID()));
        adapter.actualizarSesion(new ActualizarSesionRepositoryDTO(
                UUID.randomUUID(), "Sesion", LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                "Aula 1", "Descripcion", UUID.randomUUID(), UUID.randomUUID()));
        adapter.cerrarSesion(new CerrarSesionRepositoryDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Cerrada a tiempo", UUID.randomUUID()));
        adapter.generarSesionesGrupo(new GenerarSesionesGrupoRepositoryDTO(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void consultarSesion_con_sesion_cerrada_retorna_datos_de_cierre() {
        final SesionRepositoryProjection resultado = adapter.consultarSesion(
                new ConsultarSesionRepositoryDTO(SESION_CERRADA));

        assertEquals(SESION_CERRADA, resultado.getSesion());
        assertEquals("Sesion de cierre de periodo", resultado.getNombre());
        assertEquals(16, resultado.getNumero());
        assertEquals("CIERRE-16", resultado.getCodigo());
    }

    @Test
    void consultarSesion_con_sesion_abierta_retorna_datos_de_seguimiento() {
        final UUID sesion = UUID.randomUUID();
        final SesionRepositoryProjection resultado = adapter.consultarSesion(
                new ConsultarSesionRepositoryDTO(sesion));

        assertEquals(sesion, resultado.getSesion());
        assertEquals("Sesion de seguimiento academico", resultado.getNombre());
        assertEquals(3, resultado.getNumero());
        assertEquals("SES-03", resultado.getCodigo());
    }
}
