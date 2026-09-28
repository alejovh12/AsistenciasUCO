package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Canal tecnico machine-readable {@code DBCODE=<CODIGO>|<detalle>} publicado por los SP de negocio. */
class DbTechnicalErrorTest {

    @Test
    void parse_extrae_codigo_y_detalle_del_formato_canonico() {
        final Optional<DbTechnicalError> parsed = DbTechnicalError.parse("DBCODE=SES_001|La sesion no existe");

        assertTrue(parsed.isPresent());
        assertEquals("SES_001", parsed.get().codigo());
        assertEquals("La sesion no existe", parsed.get().detalle());
    }

    @Test
    void parse_admite_detalle_vacio_y_multilinea() {
        assertEquals("", DbTechnicalError.parse("DBCODE=GEN_002|").orElseThrow().detalle());
        final String multilinea = "linea 1" + System.lineSeparator() + "linea 2";
        assertEquals(multilinea, DbTechnicalError.parse("DBCODE=GEN_002|" + multilinea).orElseThrow().detalle());
    }

    @Test
    void parse_devuelve_vacio_para_mensajes_nulos_o_fuera_de_formato() {
        assertTrue(DbTechnicalError.parse(null).isEmpty());
        assertTrue(DbTechnicalError.parse("").isEmpty());
        assertTrue(DbTechnicalError.parse("sin marcador").isEmpty());
        assertTrue(DbTechnicalError.parse("DBCODE=minuscula|detalle").isEmpty());
        assertTrue(DbTechnicalError.parse("DBCODE=SES_001 sin separador").isEmpty());
        assertTrue(DbTechnicalError.parse("prefijo DBCODE=SES_001|detalle").isEmpty());
    }

    @Test
    void hasDbCodeMarker_detecta_el_prefijo_aunque_el_formato_sea_invalido() {
        assertTrue(DbTechnicalError.hasDbCodeMarker("DBCODE=SES_001|detalle"));
        assertTrue(DbTechnicalError.hasDbCodeMarker("DBCODE=invalido"));
        assertFalse(DbTechnicalError.hasDbCodeMarker("otro texto"));
        assertFalse(DbTechnicalError.hasDbCodeMarker(""));
        assertFalse(DbTechnicalError.hasDbCodeMarker(null));
    }
}
