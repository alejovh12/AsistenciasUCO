package co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ATA D10 (HU026B): tomas en falta que el estudiante autenticado aun puede reclamar (sesiones ya avisadas por correo, sin reclamo radicado,
 * con plazo vigente y matricula inscrita). El perfil ESTUDIANTE y la pagina los valida la DB ({@code SEC_001}, {@code PAG_001}).
 */
public interface ConsultarFaltasReclamablesInputPort
        extends InteractorWithReturn<ConsultarFaltasReclamablesInputPort.Comando, ConsultarFaltasReclamablesInputPort.Resultado> {

    /** {@code pagina} y {@code tamano} nulos toman 1 y 20. */
    record Comando(UUID usuarioId, Integer pagina, Integer tamano) {
    }

    /** Una toma reclamable; las horas son instantes UTC y {@code fechaLimiteReclamoUtc} es el instante exclusivo del plazo. */
    record FaltaReclamable(UUID idGrupo, String nombreGrupo, UUID idAsignatura, String nombreAsignatura, UUID idSesion, int numeroSesion,
                           Instant fechaHoraInicioUtc, Instant fechaHoraFinUtc, UUID idToma, int numeroToma, Instant fechaPuntoControlUtc,
                           Instant fechaLimiteReclamoUtc) {
    }

    record Resultado(List<FaltaReclamable> elementos, int pagina, int tamano, long total) {
        public Resultado {
            elementos = elementos == null ? List.of() : List.copyOf(elementos);
        }
    }
}
