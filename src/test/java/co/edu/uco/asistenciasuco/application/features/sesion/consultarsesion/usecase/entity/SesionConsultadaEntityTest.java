package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesionConsultadaEntityTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 20, 8, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 20, 10, 0);

    @Test
    void construye_entidad_valida_y_normaliza_textos() {
        final SesionConsultadaEntity entity = new SesionConsultadaEntity(
                SESION,
                GRUPO,
                "  Clase inicial  ",
                1,
                "  SES-001  ",
                3,
                "  G1  ",
                "  Grupo 1  ",
                INICIO,
                FIN
        );

        assertEquals("Clase inicial", entity.getNombre());
        assertEquals(1, entity.getNumero());
        assertEquals("SES-001", entity.getCodigo());
        assertEquals(3, entity.getNumeroSemana());
        assertEquals("G1", entity.getCodigoGrupo());
        assertEquals("Grupo 1", entity.getNombreGrupo());
        assertEquals(INICIO, entity.getFechaHoraInicio());
        assertEquals(FIN, entity.getFechaHoraFin());
    }

    @Test
    void permite_campos_opcionales_vacios() {
        final SesionConsultadaEntity entity = new SesionConsultadaEntity(
                SESION,
                GRUPO,
                "Clase inicial",
                null,
                "   ",
                null,
                "   ",
                "   ",
                null,
                null
        );

        assertNull(entity.getNumero());
        assertNull(entity.getNumeroSemana());
        assertNotNull(entity.getCodigo());
        assertEquals("", entity.getCodigo());
        assertNotNull(entity.getCodigoGrupo());
        assertEquals("", entity.getCodigoGrupo());
        assertNotNull(entity.getNombreGrupo());
        assertEquals("", entity.getNombreGrupo());
        assertNull(entity.getFechaHoraInicio());
        assertNull(entity.getFechaHoraFin());
    }

    @Test
    void rechaza_identificadores_y_textos_invalidos() {
        assertThrows(ValidationException.class, () -> entidad(null, GRUPO, "Clase inicial"));
        assertThrows(ValidationException.class, () -> entidad(SESION, null, "Clase inicial"));
        assertThrows(ValidationException.class, () -> entidad(SESION, GRUPO, null));
        assertThrows(ValidationException.class, () -> entidad(SESION, GRUPO, "   "));
        assertThrows(ValidationException.class, () -> entidad(SESION, GRUPO, "a".repeat(51)));
    }

    /** LB-001B.4A: los codigos ERR_TEMA_SESION_* se renombran a ERR_NOMBRE_SESION_*. */
    @Test
    void usa_codigos_de_nombre_de_sesion() {
        assertEquals("ERR_NOMBRE_SESION_REQUERIDO",
                assertThrows(ValidationException.class, () -> entidad(SESION, GRUPO, "   ")).getCode());
        assertEquals("ERR_NOMBRE_SESION_LONGITUD_INVALIDA",
                assertThrows(ValidationException.class, () -> entidad(SESION, GRUPO, "a".repeat(51))).getCode());
    }

    /** LB-001B.4B (TD-048): contrato de lectura maximo 50 (dbo.Sesion.nombre NVARCHAR(50)). */
    @Test
    void nombre_acepta_50_y_rechaza_51() {
        assertEquals("a".repeat(50), entidad(SESION, GRUPO, "a".repeat(50)).getNombre());
        assertEquals("a", entidad(SESION, GRUPO, "a").getNombre());
        assertEquals("ERR_NOMBRE_SESION_LONGITUD_INVALIDA",
                assertThrows(ValidationException.class, () -> entidad(SESION, GRUPO, "a".repeat(51))).getCode());
    }

    private SesionConsultadaEntity entidad(final UUID sesion, final UUID grupo, final String nombre) {
        return new SesionConsultadaEntity(
                sesion,
                grupo,
                nombre,
                1,
                "SES-001",
                3,
                "G1",
                "Grupo 1",
                INICIO,
                FIN
        );
    }
}
