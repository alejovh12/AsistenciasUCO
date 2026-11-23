package co.edu.uco.notificaciones.domain;

/** Error de negocio con codigo estable; la capa REST lo traduce a {code, message, correlationId}. */
public class ErrorNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Categoria que decide el estado HTTP. */
    public enum Categoria { VALIDACION, NO_ENCONTRADO, CONFLICTO }

    private final Categoria categoria;
    private final String codigo;

    public ErrorNegocio(final Categoria categoria, final String codigo, final String mensaje) {
        super(mensaje);
        this.categoria = categoria;
        this.codigo = codigo;
    }

    public Categoria categoria() {
        return categoria;
    }

    public String codigo() {
        return codigo;
    }

    public static ErrorNegocio validacion(final String codigo, final String mensaje) {
        return new ErrorNegocio(Categoria.VALIDACION, codigo, mensaje);
    }

    public static ErrorNegocio noEncontrado(final String codigo, final String mensaje) {
        return new ErrorNegocio(Categoria.NO_ENCONTRADO, codigo, mensaje);
    }

    public static ErrorNegocio conflicto(final String codigo, final String mensaje) {
        return new ErrorNegocio(Categoria.CONFLICTO, codigo, mensaje);
    }
}
