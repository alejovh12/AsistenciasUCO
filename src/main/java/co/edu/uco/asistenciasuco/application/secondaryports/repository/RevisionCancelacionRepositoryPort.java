package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.DestinatarioRevision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 C.1 (HU026C): decision del docente, resolucion del coordinador o del decano escalado, bandejas y destinatarios de
 * la revision de cancelacion por inasistencia. El ambito y las transiciones los decide la DB.
 */
public interface RevisionCancelacionRepositoryPort {

    ResultadoDecision decidir(UUID revisionId, String decision, String motivo, UUID usuarioId);

    ResultadoResolucion resolver(UUID revisionId, String decision, String motivo, UUID usuarioId);

    PaginaRevisiones consultarCoordinador(UUID usuarioId, int pagina, int tamano, boolean soloPendientes, String estado, UUID programaId, UUID grupoId);

    PaginaRevisiones consultarDecano(UUID usuarioId, int pagina, int tamano, boolean soloPendientes, String estado, UUID programaId);

    List<DestinatarioRevision> consultarDestinatarios(UUID usuarioId, UUID revisionId);





}
