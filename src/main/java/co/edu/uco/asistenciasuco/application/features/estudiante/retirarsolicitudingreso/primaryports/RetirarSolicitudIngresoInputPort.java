package co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.dto.RetirarSolicitudIngresoDTO;

public interface RetirarSolicitudIngresoInputPort {

    void execute(RetirarSolicitudIngresoDTO dto);
}
