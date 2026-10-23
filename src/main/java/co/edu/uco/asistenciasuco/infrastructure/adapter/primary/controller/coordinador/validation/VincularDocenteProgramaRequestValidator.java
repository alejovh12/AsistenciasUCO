package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationErrorType;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResult;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationResultBuilder;
import co.edu.uco.asistenciasuco.crosscutting.validation.Validator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.VincularDocenteProgramaRequest;

import java.util.UUID;
import java.util.regex.Pattern;

/** HU091: exactamente un modo; el modo nuevo exige datos completos y contrasena conforme a la politica (sin valor por defecto). */
public final class VincularDocenteProgramaRequestValidator implements Validator<VincularDocenteProgramaRequest> {

    private static final int MAX_NOMBRE = 50;
    private static final int MAX_CORREO = 100;
    private static final Pattern DIGITOS = Pattern.compile("\\d+");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    @Override
    public ValidationResult validate(final VincularDocenteProgramaRequest request) {
        final ValidationResultBuilder builder = new ValidationResultBuilder();
        if (request == null) {
            return builder.add("request", ValidationErrorType.REQUIRED, "La informacion del docente es obligatoria.").build();
        }
        validarUuid(builder, "idTipoVinculacion", request.getIdTipoVinculacion(), true, "El tipo de vinculacion es obligatorio.");
        validarUuid(builder, "idDocente", request.getIdDocente(), false, null);
        validarUuid(builder, "idUsuario", request.getIdUsuario(), false, null);

        final boolean hayDatosNuevos = hayTexto(request.getNumeroIdentificacion()) || hayTexto(request.getPrimerNombre())
                || hayTexto(request.getPrimerApellido()) || hayTexto(request.getCorreo()) || hayTexto(request.getPassword())
                || request.getIdTipoIdentificacion() != null;
        final int modos = (request.getIdDocente() != null ? 1 : 0) + (request.getIdUsuario() != null ? 1 : 0) + (hayDatosNuevos ? 1 : 0);
        if (modos == 0) {
            builder.add("modo", ValidationErrorType.REQUIRED, "Debe indicar un docente existente o los datos de un docente nuevo.");
        } else if (modos > 1) {
            builder.add("modo", ValidationErrorType.INVALID_VALUE, "Debe indicar un docente existente o los datos de un docente nuevo, pero no ambos.");
        } else if (hayDatosNuevos) {
            validarPersonaNueva(builder, request);
        }
        return builder.build();
    }

    /** Dominio con al menos un punto y sin etiquetas vacias; se valida en codigo para mantener el patron lineal. */
    private static boolean dominioValido(final String correo) {
        final String dominio = correo.substring(correo.indexOf('@') + 1);
        return dominio.indexOf('.') > 0 && !dominio.endsWith(".") && !dominio.contains("..");
    }

    private static void validarPersonaNueva(final ValidationResultBuilder builder, final VincularDocenteProgramaRequest request) {
        validarUuid(builder, "idTipoIdentificacion", request.getIdTipoIdentificacion(), true, "El tipo de identificacion es obligatorio.");
        Integer numero = null;
        final String documento = request.getNumeroIdentificacion() == null ? null : request.getNumeroIdentificacion().trim();
        if (!hayTexto(documento)) {
            builder.add("numeroIdentificacion", ValidationErrorType.REQUIRED, "El numero de identificacion es obligatorio.");
        } else if (!DIGITOS.matcher(documento).matches()) {
            builder.add("numeroIdentificacion", ValidationErrorType.INVALID_FORMAT, "El numero de identificacion debe ser numerico.");
        } else {
            try {
                numero = Integer.valueOf(documento);
            } catch (final NumberFormatException exception) {
                builder.add("numeroIdentificacion", ValidationErrorType.OUT_OF_RANGE, "El numero de identificacion excede el rango permitido.");
            }
        }
        requeridoConLongitud(builder, "primerNombre", request.getPrimerNombre(), MAX_NOMBRE, true);
        requeridoConLongitud(builder, "primerApellido", request.getPrimerApellido(), MAX_NOMBRE, true);
        requeridoConLongitud(builder, "segundoNombre", request.getSegundoNombre(), MAX_NOMBRE, false);
        requeridoConLongitud(builder, "segundoApellido", request.getSegundoApellido(), MAX_NOMBRE, false);

        final String correo = request.getCorreo() == null ? null : request.getCorreo().trim();
        if (!hayTexto(correo)) {
            builder.add("correo", ValidationErrorType.REQUIRED, "El correo es obligatorio.");
        } else if (correo.length() > MAX_CORREO) {
            builder.add("correo", ValidationErrorType.INVALID_LENGTH, "El correo excede la longitud permitida.");
        } else if (!CORREO.matcher(correo).matches() || !dominioValido(correo)) {
            builder.add("correo", ValidationErrorType.INVALID_FORMAT, "El correo no tiene un formato valido.");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            builder.add("password", ValidationErrorType.REQUIRED, "La contrasena inicial es obligatoria.");
        } else {
            try {
                PasswordRegistroRule.resolverCredencialNueva(request.getPassword(), numero == null ? 0 : numero);
            } catch (final ValidationException exception) {
                builder.add("password", ValidationErrorType.INVALID_FORMAT, "La contrasena no cumple la politica de seguridad.");
            }
        }
    }

    private static void requeridoConLongitud(final ValidationResultBuilder builder, final String campo, final String valor,
                                             final int max, final boolean requerido) {
        if (!hayTexto(valor)) {
            if (requerido) {
                builder.add(campo, ValidationErrorType.REQUIRED, "El campo " + campo + " es obligatorio.");
            }
        } else if (valor.trim().length() > max) {
            builder.add(campo, ValidationErrorType.INVALID_LENGTH, "El campo " + campo + " excede la longitud permitida.");
        }
    }

    private static void validarUuid(final ValidationResultBuilder builder, final String campo, final UUID valor,
                                    final boolean requerido, final String mensajeRequerido) {
        if (valor == null) {
            if (requerido) {
                builder.add(campo, ValidationErrorType.REQUIRED, mensajeRequerido);
            }
        } else if (valor.getMostSignificantBits() == 0 && valor.getLeastSignificantBits() == 0) {
            builder.add(campo, ValidationErrorType.INVALID_UUID, "El identificador " + campo + " no es valido.");
        }
    }

    private static boolean hayTexto(final String valor) {
        return valor != null && !valor.isBlank();
    }
}
