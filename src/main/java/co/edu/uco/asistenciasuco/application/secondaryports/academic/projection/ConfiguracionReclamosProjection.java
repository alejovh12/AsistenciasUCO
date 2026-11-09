package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

/**
 * Configuracion institucional del formulario de reclamos (CULM-001 D.2): una fila de usp_consultar_configuracion_reclamos.
 * Los valores nulos se conservan para que el caso de uso decida (una configuracion incompleta es un fallo interno).
 * {@code tiposPermitidos} son los tipos de contenido separados por coma.
 */
public record ConfiguracionReclamosProjection(
        Integer maximoSoportes,
        Long tamanioMaximoBytes,
        String tiposPermitidos,
        Integer limiteSubidasPorMinuto,
        Integer diasLimiteJustificacion
) {
}
