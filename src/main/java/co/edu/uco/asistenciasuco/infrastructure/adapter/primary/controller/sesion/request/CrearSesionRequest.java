package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;

public final class CrearSesionRequest {

    @NonEmptyUuid(message = "El identificador del grupo no es valido.")
    @NotNull(message = "El grupo es obligatorio.")
    private UUID grupo;
    @NotBlank(message = "El nombre es obligatorio.")
    private String nombre;
    private String fechaHoraInicio;
    private String fechaHoraFin;

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }

    public String getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(final String fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public String getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(final String fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }
}
