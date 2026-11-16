package co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.usecase;

import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort.MensajeInterfaz;

/** INT-003: consulta un texto de la interfaz en el catalogo de mensajes. */
public interface ConsultarMensajeInterfazUseCase {

    MensajeInterfaz execute(String codigo);
}
