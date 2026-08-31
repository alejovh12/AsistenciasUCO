package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

public final class HttpValidationMessages {

    public static final String REQUIRED = "El campo es obligatorio.";
    public static final String INVALID_PERSON_NAME = "Solo se permiten letras, espacios, apostrofes y guiones.";
    public static final String INVALID_EMAIL = "Ingrese un correo valido, por ejemplo nombre@dominio.com.";
    public static final String INVALID_UUID = "El identificador no es valido.";
    public static final String INVALID_IDENTIFICATION_NUMBER = "El numero de identificacion debe ser un numero positivo valido.";
    public static final String INVALID_PAGE = "La pagina debe ser mayor o igual que cero.";
    public static final String INVALID_PAGE_SIZE = "El tamano de pagina debe estar entre 1 y 100.";

    private HttpValidationMessages() {
    }
}
