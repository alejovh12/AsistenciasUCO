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

    /**
     * Exactamente uno de {@code token} o {@code pin}. {@code idPrograma} (DEC-078, opcional): programa que elige el estudiante
     * al matricularse en un grupo compartido; la validacion lo ignora.
     */
    public record Credencial(
            @Pattern(regexp = BeanValidationPatterns.BASE64URL_43_STRICT_OR_BLANK, message = "El código de matricula no es válido.") String token,
            @Pattern(regexp = BeanValidationPatterns.PIN_6_STRICT_OR_BLANK, message = "El PIN debe tener 6 digitos.") String pin,
            UUID idPrograma
    ) {
        public Credencial(final String token, final String pin) {
            this(token, pin, null);
        }
    }

    /** Alta publica: credencial (token o PIN) mas los datos de la persona. */
    public record Registro(
            @Pattern(regexp = BeanValidationPatterns.BASE64URL_43_STRICT_OR_BLANK, message = "El código de matricula no es válido.") String token,
            @Pattern(regexp = BeanValidationPatterns.PIN_6_STRICT_OR_BLANK, message = "El PIN debe tener 6 digitos.") String pin,
            @NotNull(message = "El tipo de identificación es obligatorio.") UUID idTipoIdentificacion,
            @NotNull(message = "El número de identificación es obligatorio.") @Min(value = 100000, message = "El número de identificación no es válido.") @Max(value = 2147483647, message = "El número de identificación no es válido.") Long numeroIdentificacion,
            @NotBlank(message = "El campo primerNombre es obligatorio.") String primerNombre,
            String segundoNombre,
            @NotBlank(message = "El campo primerApellido es obligatorio.") String primerApellido,
            String segundoApellido,
            @NotBlank(message = "El correo es obligatorio.") String correo,
            @NotBlank(message = "La contraseña es obligatoria.") @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres.") String password,
            // DEC-078: programa que elige el estudiante en un grupo compartido (opcional; la BD lo valida contra el grupo).
            UUID idPrograma
    ) {
        public Registro(final String token, final String pin, final UUID idTipoIdentificacion, final Long numeroIdentificacion,
                        final String primerNombre, final String segundoNombre, final String primerApellido,
                        final String segundoApellido, final String correo, final String password) {
            this(token, pin, idTipoIdentificacion, numeroIdentificacion, primerNombre, segundoNombre, primerApellido,
                    segundoApellido, correo, password, null);
        }
    }
}
