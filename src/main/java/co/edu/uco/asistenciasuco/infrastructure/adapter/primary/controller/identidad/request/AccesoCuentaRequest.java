package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationPatterns;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuenta cuyo estado de acceso se consulta o reconcilia, identificada por su correo institucional. */
public final class AccesoCuentaRequest {

    @NotBlank(message = "El correo es obligatorio.")
    @Size(max = 100, message = "El correo puede tener maximo 100 caracteres.")
    @Pattern(regexp = BeanValidationPatterns.EMAIL_OR_BLANK, message = HttpValidationMessages.INVALID_EMAIL)
    private String correo;

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(final String correo) {
        this.correo = correo;
    }
}
