package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarAsistenciasSesionDomainTest {

    @Test
    void constructor_con_datos_validos_copia_registros() {
        final UUID sesion = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final RegistroAsistenciaSesionDomain registro =
                new RegistroAsistenciaSesionDomain(UUID.randomUUID(), "AN");

        final RegistrarAsistenciasSesionDomain domain =
                new RegistrarAsistenciasSesionDomain(sesion, List.of(registro), usuarioEjecutor);

        assertEquals(sesion, domain.getSesion());
        assertEquals(1, domain.getRegistros().size());
        assertEquals(usuarioEjecutor, domain.getUsuarioEjecutor());
    }

    @Test
    void constructor_rechaza_sesion_nula() {
        final RegistroAsistenciaSesionDomain registro =
                new RegistroAsistenciaSesionDomain(UUID.randomUUID(), "AN");

        final var registrosPrimero = List.of(registro);
        final var idAleatorio2 = UUID.randomUUID();
        assertThrows(ValidationException.class,
                () -> new RegistrarAsistenciasSesionDomain(null, registrosPrimero, idAleatorio2));
    }

    @Test
    void constructor_rechaza_registros_nulos_o_vacios() {
        final UUID sesion = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();

        assertThrows(ValidationException.class, () -> new RegistrarAsistenciasSesionDomain(sesion, null, usuarioEjecutor));
        assertThrows(ValidationException.class, () -> new RegistrarAsistenciasSesionDomain(sesion, List.of(), usuarioEjecutor));
    }

    @Test
    void constructor_rechaza_usuario_ejecutor_nulo() {
        final RegistroAsistenciaSesionDomain registro =
                new RegistroAsistenciaSesionDomain(UUID.randomUUID(), "AN");

        final var idAleatorio = UUID.randomUUID();
        final var registrosSegundo = List.of(registro);
        assertThrows(ValidationException.class,
                () -> new RegistrarAsistenciasSesionDomain(idAleatorio, registrosSegundo, null));
    }
}
