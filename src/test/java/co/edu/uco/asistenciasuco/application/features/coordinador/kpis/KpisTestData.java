package co.edu.uco.asistenciasuco.application.features.coordinador.kpis;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AlertaTempranaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.BitacoraSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FichaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenAsignaturaProjection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Datos de prueba compartidos por los casos de uso de KPIs del coordinador (HU065-HU070). */
public final class KpisTestData {

    public static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PROGRAMA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID ESTUDIANTE = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID GRUPO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID ASIGNATURA = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final UUID SESION = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private KpisTestData() {
    }

    public static KpisCoordinadorProjection kpis() {
        return new KpisCoordinadorProjection(PROGRAMA, "Ingenieria de Sistemas", PERIODO, "2026-2", 5, 2, 1, 2, new BigDecimal("60.00"),
                15, 10, 9, 1, 1, new BigDecimal("90.00"), new BigDecimal("66.67"), new BigDecimal("15.00"), new BigDecimal("20.00"));
    }

    public static AlertaTempranaProjection alerta() {
        return new AlertaTempranaProjection(ESTUDIANTE, "Ana Perez", 1017000001, "ana@uco.edu.co", 2, new BigDecimal("66.67"), 2, "PERDIDA", "ROJO");
    }

    public static BitacoraSesionProjection bitacora() {
        return new BitacoraSesionProjection(SESION, 7, "Sesion G1-7", Instant.parse("2026-10-23T14:00:00Z"), Instant.parse("2026-10-23T16:00:00Z"),
                GRUPO, 931001, ASIGNATURA, "Estructuras de Datos", null, null, null);
    }

    public static FichaEstudianteProjection ficha() {
        return new FichaEstudianteProjection(ESTUDIANTE, "CC", 1017000001, "Ana", "Maria", "Perez", "Gomez", "Ana Maria Perez Gomez",
                "ana@uco.edu.co", true, PROGRAMA, "Ingenieria de Sistemas");
    }

    public static ResumenAsignaturaProjection resumen() {
        return new ResumenAsignaturaProjection(GRUPO, 931001, ASIGNATURA, "Estructuras de Datos", "A", "activo", 6, 4, 2, 0, new BigDecimal("33.33"), "PERDIDA");
    }
}
