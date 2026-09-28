package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface CierrePeriodoCommandPort {

    void ejecutarCierreMasivoPeriodo(String codigoPeriodo, String idActor, UUID idUsuarioEjecutor);
}
