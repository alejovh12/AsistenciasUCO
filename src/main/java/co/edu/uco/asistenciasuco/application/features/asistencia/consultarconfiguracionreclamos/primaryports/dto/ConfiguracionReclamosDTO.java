package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.dto;

import java.util.List;

/**
 * Configuracion institucional del formulario de reclamos: tope de soportes, tamano maximo por archivo, tipos de contenido
 * aceptados, limite de subidas por minuto y dias del plazo de justificacion. Sale de la base de datos; el cliente no la duplica.
 */
public record ConfiguracionReclamosDTO(
        int maximoSoportes,
        long tamanioMaximoBytes,
        List<String> tiposPermitidos,
        int limiteSubidasPorMinuto,
        int diasLimiteJustificacion
) {
}
