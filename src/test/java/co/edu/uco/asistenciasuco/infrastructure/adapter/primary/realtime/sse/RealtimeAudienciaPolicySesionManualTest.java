package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy.Audiencia;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 / DEC-064 y DEC-065 (2): {@code SESION_MANUAL_CREADA} llega a los estudiantes del grupo, al ambito de lectura (el
 * coordinador del programa) y al docente titular (que ve aprobada su solicitud al instante); {@code SESION_SOLICITUD_CREADA} solo
 * al ambito y al titular, nunca a los estudiantes. Igualdad exacta del tipo.
 */
class RealtimeAudienciaPolicySesionManualTest {

    private static final String TIPO = "SESION_MANUAL_CREADA";
    private final RealtimeAudienciaPolicy policy = RealtimeAudienciaPolicy.porDefecto();

    @Test
    void audiencia_de_estudiantes_ambito_del_coordinador_y_docente_titular() {
        assertEquals(EnumSet.of(Audiencia.ESTUDIANTE_GRUPO, Audiencia.AMBITO_GRUPO, Audiencia.DOCENTE_TITULAR), policy.audienciasDe(TIPO));
        assertTrue(policy.entregaA(TIPO, Audiencia.ESTUDIANTE_GRUPO));
        assertTrue(policy.entregaA(TIPO, Audiencia.AMBITO_GRUPO));
        assertTrue(policy.entregaA(TIPO, Audiencia.DOCENTE_TITULAR));
        assertFalse(policy.entregaA(TIPO, Audiencia.ADMINISTRADOR));
        assertTrue(policy.tiposConocidos().contains(TIPO));
    }

    @Test
    void entrega_por_suscriptor() {
        assertTrue(policy.entrega(TIPO, Set.of(Audiencia.ESTUDIANTE_GRUPO), null, Set.of()));
        assertTrue(policy.entrega(TIPO, Set.of(Audiencia.AMBITO_GRUPO), null, Set.of()));
        assertTrue(policy.entrega(TIPO, Set.of(Audiencia.DOCENTE_TITULAR), null, Set.of()));
        assertFalse(policy.entrega(TIPO, Set.of(Audiencia.ADMINISTRADOR), null, Set.of()));
    }

    @Test
    void la_solicitud_creada_llega_al_ambito_y_al_titular_pero_no_a_los_estudiantes() {
        final String solicitud = "SESION_SOLICITUD_CREADA";
        assertEquals(EnumSet.of(Audiencia.AMBITO_GRUPO, Audiencia.DOCENTE_TITULAR), policy.audienciasDe(solicitud));
        assertTrue(policy.entregaA(solicitud, Audiencia.AMBITO_GRUPO));
        assertTrue(policy.entregaA(solicitud, Audiencia.DOCENTE_TITULAR));
        assertFalse(policy.entregaA(solicitud, Audiencia.ESTUDIANTE_GRUPO));
        assertFalse(policy.entrega(solicitud, Set.of(Audiencia.ESTUDIANTE_GRUPO), null, Set.of()));
        assertTrue(policy.tiposConocidos().contains(solicitud));
        assertEquals(Set.of(Audiencia.DOCENTE_TITULAR), policy.audienciasDe("SESION_SOLICITUD_CREADA_X"));
    }

    @Test
    void un_tipo_parecido_no_hereda_la_audiencia() {
        assertEquals(Set.of(Audiencia.DOCENTE_TITULAR), policy.audienciasDe("SESION_MANUAL_CREADA_X"));
        assertEquals(Set.of(Audiencia.DOCENTE_TITULAR), policy.audienciasDe("sesion_manual_creada"));
    }
}
