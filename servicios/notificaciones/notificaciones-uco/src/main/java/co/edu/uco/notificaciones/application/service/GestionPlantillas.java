package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.TipoPlantillaAdmin;
import co.edu.uco.notificaciones.application.model.VistaPrevia;
import co.edu.uco.notificaciones.application.port.in.PlantillasUseCase;
import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.domain.ContenidoRenderizado;
import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.TipoNotificacion;
import co.edu.uco.notificaciones.domain.VersionPlantilla;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Casos de uso de plantillas: listar con marcadores e historial, crear version (queda activa), activar y vista previa. */
public class GestionPlantillas implements PlantillasUseCase {

    private static final String IDIOMA_DEFECTO = "es";

    private final PlantillaRepositoryPort repositorio;
    private final RenderizadorPlantillas renderizador;
    private final Clock clock;

    public GestionPlantillas(final PlantillaRepositoryPort repositorio, final RenderizadorPlantillas renderizador, final Clock clock) {
        this.repositorio = Objects.requireNonNull(repositorio, "PlantillaRepositoryPort es obligatorio.");
        this.renderizador = Objects.requireNonNull(renderizador, "RenderizadorPlantillas es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public List<TipoPlantillaAdmin> consultar(final String idioma) {
        final String idiomaNorm = normalizarIdioma(idioma);
        final Map<String, List<Marcador>> marcadores = repositorio.marcadoresPorTipo();
        final Map<String, List<VersionPlantilla>> versiones = repositorio.versionesPorTipo(idiomaNorm);
        return repositorio.listarTipos().stream().map(tipo -> {
            final List<VersionPlantilla> historial = versiones.getOrDefault(tipo.tipo(), List.of());
            final VersionPlantilla activa = historial.stream().filter(VersionPlantilla::activa).findFirst().orElse(null);
            return new TipoPlantillaAdmin(
                    tipo.tipo(),
                    tipo.descripcion(),
                    idiomaNorm,
                    activa == null ? 0 : activa.version(),
                    activa == null ? "" : activa.asunto(),
                    activa == null ? "" : activa.cuerpo(),
                    activa == null ? clock.instant() : activa.creadoEn(),
                    marcadores.getOrDefault(tipo.tipo(), List.of()),
                    historial);
        }).toList();
    }

    @Override
    public VersionPlantilla crearVersion(final String tipo, final String idioma, final String asunto, final String cuerpo, final String creadoPor) {
        final TipoNotificacion conocido = exigirTipo(tipo);
        final String asuntoNorm = asunto == null ? "" : asunto.trim();
        final String cuerpoNorm = cuerpo == null ? "" : cuerpo.trim();
        ValidadorPlantilla.validar(repositorio.marcadores(conocido.tipo()), asuntoNorm, cuerpoNorm);
        final String autor = creadoPor == null || creadoPor.isBlank() ? null : creadoPor.trim();
        return repositorio.crearYActivar(conocido.tipo(), normalizarIdioma(idioma), asuntoNorm, cuerpoNorm, autor, clock.instant());
    }

    @Override
    public VersionPlantilla activarVersion(final String tipo, final String idioma, final int version) {
        final TipoNotificacion conocido = exigirTipo(tipo);
        return repositorio.activar(conocido.tipo(), normalizarIdioma(idioma), version).orElseThrow(() ->
                ErrorNegocio.noEncontrado("VERSION_NO_ENCONTRADA", "No existe la version de plantilla solicitada para el tipo e idioma indicados."));
    }

    @Override
    public VistaPrevia vistaPrevia(final String tipo, final String idioma, final String asunto, final String cuerpo) {
        final TipoNotificacion conocido = exigirTipo(tipo);
        final Map<String, String> muestras = new LinkedHashMap<>();
        final List<Marcador> marcadores = repositorio.marcadores(conocido.tipo());
        if (marcadores.isEmpty()) {
            muestras.putAll(MuestrasNotificacion.basicoVistaPrevia());
        }
        for (final Marcador m : marcadores) {
            muestras.put(m.marcador(), m.ejemplo() == null || m.ejemplo().isBlank() ? MuestrasNotificacion.valorPara(m.marcador()) : m.ejemplo());
        }
        final ContenidoRenderizado r = renderizador.renderizarExplicito(asunto, cuerpo, muestras);
        return new VistaPrevia(r.asunto(), r.textoPlano(), r.html(), r.origen());
    }

    private TipoNotificacion exigirTipo(final String tipo) {
        final String limpio = tipo == null ? "" : tipo.trim();
        return repositorio.buscarTipo(limpio)
                .filter(t -> !t.interno())
                .orElseThrow(() -> ErrorNegocio.noEncontrado("TIPO_NO_ENCONTRADO", "El tipo de plantilla no existe."));
    }

    private static String normalizarIdioma(final String idioma) {
        return idioma == null || idioma.isBlank() ? IDIOMA_DEFECTO : idioma.trim();
    }
}
