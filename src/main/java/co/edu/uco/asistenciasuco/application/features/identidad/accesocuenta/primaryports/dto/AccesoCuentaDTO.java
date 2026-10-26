package co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto;

import java.util.List;
import java.util.UUID;

/** Estado de acceso de una cuenta y detalle por rol. {@code detalle} es texto técnico sin datos sensibles. */
public record AccesoCuentaDTO(UUID usuario, EstadoAccesoCuenta estado, String detalle, List<AccesoRolDTO> roles) {
}
