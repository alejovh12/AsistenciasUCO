package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy.Audiencia;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ATA D10: {@code FALTA_SESION_AVISADA} solo se entrega a los estudiantes avisados (audiencia DESTINATARIO); ni el titular, ni el ambito
 * de lectura ni los demas estudiantes del grupo la reciben. Igualdad exacta del tipo.
 */
class RealtimeAudienciaPolicyFaltaAvisadaTest {

    private static final String TIPO = "FALTA_SESION_AVISADA";
    private static final UUID AVISADO = UUID.randomUUID();
    private static final UUID OTRO = UUID.randomUUID();
    private final RealtimeAudienciaPolicy policy = RealtimeAudienciaPolicy.porDefecto();

    @Test
    void la_audiencia_es_solo_destinatario() {
        assertEquals(EnumSet.of(Audiencia.DESTINATARIO), policy.audienciasDe(TIPO));
        assertTrue(policy.tiposConocidos().contains(TIPO));
        assertFalse(policy.entregaA(TIPO, Audiencia.DOCENTE_TITULAR));
        assertFalse(policy.entregaA(TIPO, Audiencia.ESTUDIANTE_GRUPO));
        assertFalse(policy.entregaA(TIPO, Audiencia.AMBITO_GRUPO));
    }

    @Test
    void solo_el_destinatario_explicito_recibe_el_evento() {
        final Set<UUID> destinatarios = Set.of(AVISADO);
        assertTrue(policy.entrega(TIPO, Set.of(Audiencia.ESTUDIANTE_GRUPO), AVISADO, destinatarios));
        assertFalse(policy.entrega(TIPO, Set.of(Audiencia.ESTUDIANTE_GRUPO), OTRO, destinatarios));
        assertFalse(policy.entrega(TIPO, Set.of(Audiencia.DOCENTE_TITULAR), OTRO, destinatarios));
        assertFalse(policy.entrega(TIPO, Set.of(Audiencia.AMBITO_GRUPO, Audiencia.DOCENTE_TITULAR), OTRO, destinatarios));
        assertFalse(policy.entrega(TIPO, Set.of(Audiencia.ESTUDIANTE_GRUPO), AVISADO, Set.of()));
    }

    @Test
    void un_tipo_parecido_no_hereda_la_audiencia() {
        assertEquals(Set.of(Audiencia.DOCENTE_TITULAR), policy.audienciasDe("FALTA_SESION_AVISADA_X"));
        assertEquals(Set.of(Audiencia.DOCENTE_TITULAR), policy.audienciasDe("falta_sesion_avisada"));
    }
}
