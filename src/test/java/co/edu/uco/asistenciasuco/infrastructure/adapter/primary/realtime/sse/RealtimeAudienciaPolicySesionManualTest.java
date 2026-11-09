package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy.Audiencia;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 / DEC-064: {@code SESION_MANUAL_CREADA} llega a los estudiantes del grupo y al ambito de lectura (el coordinador del
 * programa); el docente que la creo ya la conoce por la respuesta HTTP. Igualdad exacta del tipo.
 */
class RealtimeAudienciaPolicySesionManualTest {

    private static final String TIPO = "SESION_MANUAL_CREADA";
    private final RealtimeAudienciaPolicy policy = RealtimeAudienciaPolicy.porDefecto();

    @Test
    void audiencia_de_estudiantes_del_grupo_y_ambito_del_coordinador() {
        assertEquals(EnumSet.of(Audiencia.ESTUDIANTE_GRUPO, Audiencia.AMBITO_GRUPO), policy.audienciasDe(TIPO));
        assertTrue(policy.entregaA(TIPO, Audiencia.ESTUDIANTE_GRUPO));
        assertTrue(policy.entregaA(TIPO, Audiencia.AMBITO_GRUPO));
        assertFalse(policy.entregaA(TIPO, Audiencia.DOCENTE_TITULAR));
        assertFalse(policy.entregaA(TIPO, Audiencia.ADMINISTRADOR));
        assertTrue(policy.tiposConocidos().contains(TIPO));
    }

    @Test
    void entrega_por_suscriptor() {
        assertTrue(policy.entrega(TIPO, Set.of(Audiencia.ESTUDIANTE_GRUPO), null, Set.of()));
        assertTrue(policy.entrega(TIPO, Set.of(Audiencia.AMBITO_GRUPO), null, Set.of()));
        assertFalse(policy.entrega(TIPO, Set.of(Audiencia.DOCENTE_TITULAR), null, Set.of()));
    }

    @Test
    void un_tipo_parecido_no_hereda_la_audiencia() {
        assertEquals(Set.of(Audiencia.DOCENTE_TITULAR), policy.audienciasDe("SESION_MANUAL_CREADA_X"));
        assertEquals(Set.of(Audiencia.DOCENTE_TITULAR), policy.audienciasDe("sesion_manual_creada"));
    }
}
