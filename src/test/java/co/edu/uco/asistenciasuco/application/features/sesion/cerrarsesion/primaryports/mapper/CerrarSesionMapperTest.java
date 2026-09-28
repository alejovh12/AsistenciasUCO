package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain.CerrarSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CerrarSesionMapperTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOCENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> CerrarSesionMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_conserva_los_campos() {
        final CerrarSesionDomain domain = CerrarSesionMapper.toDomain(
                new CerrarSesionDTO(SESION, DOCENTE, "Cierre programado de la sesion", USUARIO));

        assertEquals(SESION, domain.getSesion());
        assertEquals("Cierre programado de la sesion", domain.getObservacionCierre());
        assertEquals(USUARIO, domain.getUsuarioEjecutor());
    }

    @Test
    void toDomain_propaga_la_validacion_del_dominio_cuando_el_dto_es_invalido() {
        assertThrows(ValidationException.class,
                () -> CerrarSesionMapper.toDomain(new CerrarSesionDTO(null, DOCENTE, "Cierre programado", USUARIO)));
    }
}
