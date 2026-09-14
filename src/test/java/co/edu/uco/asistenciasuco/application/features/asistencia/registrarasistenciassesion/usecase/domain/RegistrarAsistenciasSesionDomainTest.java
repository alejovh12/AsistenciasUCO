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
        final RegistroAsistenciaSesionDomain registro =
                new RegistroAsistenciaSesionDomain(UUID.randomUUID(), "ASISTIO");

        final RegistrarAsistenciasSesionDomain domain =
                new RegistrarAsistenciasSesionDomain(sesion, List.of(registro));

        assertEquals(sesion, domain.getSesion());
        assertEquals(1, domain.getRegistros().size());
    }

    @Test
    void constructor_rechaza_sesion_nula() {
        final RegistroAsistenciaSesionDomain registro =
                new RegistroAsistenciaSesionDomain(UUID.randomUUID(), "ASISTIO");

        assertThrows(ValidationException.class,
                () -> new RegistrarAsistenciasSesionDomain(null, List.of(registro)));
    }

    @Test
    void constructor_rechaza_registros_nulos_o_vacios() {
        final UUID sesion = UUID.randomUUID();

        assertThrows(ValidationException.class, () -> new RegistrarAsistenciasSesionDomain(sesion, null));
        assertThrows(ValidationException.class, () -> new RegistrarAsistenciasSesionDomain(sesion, List.of()));
    }
}
