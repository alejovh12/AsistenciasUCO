package co.edu.uco.asistenciasuco.application.features.decano.common;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoProjection;

import java.util.UUID;

/** Datos de prueba compartidos por los casos de uso de programa academico (HU076-HU078). */
public final class ProgramaTestData {

    public static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID PROGRAMA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID TIPO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID COORDINADOR = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID MODALIDAD = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final UUID JORNADA = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private ProgramaTestData() {
    }

    public static ProgramaAcademicoProjection projection() {
        return new ProgramaAcademicoProjection(PROGRAMA, "Ingenieria de Sistemas", TIPO, "pregrado", MODALIDAD, "Presencial",
                JORNADA, "Diurna", COORDINADOR, "Ana Perez", FACULTAD, "Facultad de Ingenieria", true);
    }
}
