package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.domain;

import java.util.List;

/** Configuracion del formulario de reclamos; {@code tiposPermitidos} es una lista inmutable en el orden del catalogo. */
public record ConfiguracionReclamosDomain(
        int maximoSoportes,
        long tamanioMaximoBytes,
        List<String> tiposPermitidos,
        int limiteSubidasPorMinuto,
        int diasLimiteJustificacion
) {
}
