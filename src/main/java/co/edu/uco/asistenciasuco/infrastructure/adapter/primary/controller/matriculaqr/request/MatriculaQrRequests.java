package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request;

import java.util.UUID;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationPatterns;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpos HTTP de la matricula por QR/PIN (HU176B). Nunca se registran en logs: llevan el token y la contrasena. */
public final class MatriculaQrRequests {

    private MatriculaQrRequests() {
    }

    /** Exactamente uno de {@code token} o {@code pin}. */
    public record Credencial(
            @Pattern(regexp = BeanValidationPatterns.BASE64URL_43_STRICT_OR_BLANK, message = "El codigo de matricula no es valido.") String token,
            @Pattern(regexp = BeanValidationPatterns.PIN_6_STRICT_OR_BLANK, message = "El PIN debe tener 6 digitos.") String pin
    ) {
    }

    /** Alta publica: credencial (token o PIN) mas los datos de la persona. */
    public record Registro(
            @Pattern(regexp = BeanValidationPatterns.BASE64URL_43_STRICT_OR_BLANK, message = "El codigo de matricula no es valido.") String token,
            @Pattern(regexp = BeanValidationPatterns.PIN_6_STRICT_OR_BLANK, message = "El PIN debe tener 6 digitos.") String pin,
            @NotNull(message = "El tipo de identificacion es obligatorio.") UUID idTipoIdentificacion,
            @NotNull(message = "El numero de identificacion es obligatorio.") @Min(value = 100000, message = "El numero de identificacion no es valido.") @Max(value = 2147483647, message = "El numero de identificacion no es valido.") Long numeroIdentificacion,
            @NotBlank(message = "El campo primerNombre es obligatorio.") String primerNombre,
            String segundoNombre,
            @NotBlank(message = "El campo primerApellido es obligatorio.") String primerApellido,
            String segundoApellido,
            @NotBlank(message = "El correo es obligatorio.") String correo,
            @NotBlank(message = "La contrasena es obligatoria.") @Size(min = 8, max = 255, message = "La contrasena debe tener entre 8 y 255 caracteres.") String password
    ) {
    }
}
