package co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.primaryports.ConsultarAlertasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.usecase.ConsultarAlertasEstudianteUseCase;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AlertaFila;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AlertaInasistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.PaginaAlertas;

import java.util.Objects;

public final class ConsultarAlertasEstudianteUseCaseImpl implements ConsultarAlertasEstudianteUseCase {

    public static final int PAGINA_POR_DEFECTO = 1;
    public static final int TAMANO_POR_DEFECTO = 20;

    private final AlertaInasistenciaRepositoryPort repositorio;
    private final MessageCatalogPort mensajes;

    public ConsultarAlertasEstudianteUseCaseImpl(final AlertaInasistenciaRepositoryPort repositorio, final MessageCatalogPort mensajes) {
        this.repositorio = Objects.requireNonNull(repositorio, "AlertaInasistenciaRepositoryPort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public PaginaAlertas execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio.");
        final PaginaAlertas pagina = repositorio.consultarAlertasEstudiante(comando.usuarioId(),
                comando.pagina() == null ? PAGINA_POR_DEFECTO : comando.pagina(),
                comando.tamano() == null ? TAMANO_POR_DEFECTO : comando.tamano(),
                comando.soloNoLeidas(), comando.soloVigentes(), comando.grupoId());
        return new PaginaAlertas(pagina.filas().stream().map(fila -> fila.conMensaje(mensaje(fila))).toList(), pagina.total());
    }

    /** {@code mensaje} sale del catalogo (ALR_010/ALR_011); si el catalogo no responde se usa el texto de respaldo del mismo contrato. */
    String mensaje(final AlertaFila f) {
        final boolean perdida = "PERDIDA".equals(f.codigoTipo());
        final var umbralLimite = perdida ? f.umbral() : f.umbralPerdida();
        final String porcentaje = f.porcentajeCruce() == null ? "" : f.porcentajeCruce().toPlainString();
        final String limite = umbralLimite == null ? "" : umbralLimite.toPlainString();
        try {
            return mensajes.getUserMessage(f.codigoMensajeUsuario(), porcentaje, f.nombreAsignatura(), limite);
        } catch (final RuntimeException exception) {
            return perdida
                    ? "Alcanzaste " + porcentaje + "% de inasistencia en " + f.nombreAsignatura() + " (límite " + limite + "%). Tu docente revisará tu caso."
                    : "Llevas " + porcentaje + "% de inasistencia en " + f.nombreAsignatura() + ". Si llegas al " + limite + "% podrías perder la asignatura.";
        }
    }
}
