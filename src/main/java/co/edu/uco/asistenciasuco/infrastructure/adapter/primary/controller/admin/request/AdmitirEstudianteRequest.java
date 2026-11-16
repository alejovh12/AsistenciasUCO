package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import java.util.UUID;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationCodes;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationPatterns;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.LongitudTrasTrim;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Cuerpo de {@code POST /api/v1/admin/estudiantes/admision} (CULM-001 D.4, HU156). IDENT-002: sin contrasena (el estudiante la
 * define al activar su cuenta); como el resto de la entrada es estricta, un {@code password} de un cliente anterior es un
 * campo desconocido y responde 400. Su representacion de texto no imprime
 * datos personales (OBS-12).
 */
public final class AdmitirEstudianteRequest {
    @NonEmptyUuid(message = "El tipo de identificacion es obligatorio.", payload = BeanValidationCodes.Required.class)
    @NotNull(message = "El tipo de identificacion es obligatorio.")
    private UUID tipoIdentificacionId;
    @NotNull(message = "El numero de identificacion es obligatorio y debe ser positivo.")
    @Positive(message = "El numero de identificacion es obligatorio y debe ser positivo.", payload = BeanValidationCodes.InvalidFormat.class)
    private Integer numeroIdentificacion;
    @NotBlank(message = "El campo primerNombre es obligatorio.")
    @LongitudTrasTrim(max = 50, message = "El campo primerNombre no puede superar 50 caracteres.",
            payload = BeanValidationCodes.InvalidLength.class)
    private String primerNombre;
    @LongitudTrasTrim(max = 50, message = "El campo segundoNombre no puede superar 50 caracteres.",
            payload = BeanValidationCodes.InvalidLength.class)
    private String segundoNombre;
    @NotBlank(message = "El campo primerApellido es obligatorio.")
    @LongitudTrasTrim(max = 50, message = "El campo primerApellido no puede superar 50 caracteres.",
            payload = BeanValidationCodes.InvalidLength.class)
    private String primerApellido;
    @LongitudTrasTrim(max = 50, message = "El campo segundoApellido no puede superar 50 caracteres.",
            payload = BeanValidationCodes.InvalidLength.class)
    private String segundoApellido;
    @NotBlank(message = "El correo es obligatorio.")
    @LongitudTrasTrim(max = 255, message = "El correo no puede superar 255 caracteres.",
            payload = BeanValidationCodes.InvalidLength.class)
    @Pattern(regexp = BeanValidationPatterns.EMAIL_OR_BLANK, message = "El correo no tiene un formato valido.")
    private String correo;
    @NonEmptyUuid(message = "El programa es obligatorio.", payload = BeanValidationCodes.Required.class)
    @NotNull(message = "El programa es obligatorio.")
    private UUID idPrograma;

    public UUID getTipoIdentificacionId() { return tipoIdentificacionId; }
    public void setTipoIdentificacionId(final UUID tipoIdentificacionId) { this.tipoIdentificacionId = tipoIdentificacionId; }
    public Integer getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(final Integer numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
    public String getCorreo() { return correo; }
    public void setCorreo(final String correo) { this.correo = correo; }
    public UUID getIdPrograma() { return idPrograma; }
    public void setIdPrograma(final UUID idPrograma) { this.idPrograma = idPrograma; }

    @Override
    public String toString() {
        return "AdmitirEstudianteRequest[idPrograma=" + idPrograma + ", primerNombre=" + primerNombre + "]";
    }
}
