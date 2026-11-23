package co.edu.uco.notificaciones.soporte;

import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.TipoNotificacion;
import co.edu.uco.notificaciones.domain.VersionPlantilla;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Repositorio de plantillas en memoria para pruebas de la capa de aplicacion. */
public class PlantillaRepoFake implements PlantillaRepositoryPort {

    public final Map<String, TipoNotificacion> tipos = new LinkedHashMap<>();
    public final Map<String, List<Marcador>> marcadores = new LinkedHashMap<>();
    public final List<VersionPlantilla> versiones = new ArrayList<>();
    public boolean fallar;

    public PlantillaRepoFake tipo(final String tipo, final boolean interno, final Marcador... ms) {
        tipos.put(tipo, new TipoNotificacion(tipo, "Descripcion de " + tipo, interno));
        marcadores.put(tipo, List.of(ms));
        return this;
    }

    public PlantillaRepoFake version(final String tipo, final int version, final String asunto, final String cuerpo, final boolean activa) {
        versiones.add(new VersionPlantilla(UUID.randomUUID(), tipo, "es", version, asunto, cuerpo, activa, "sistema", Instant.parse("2026-11-01T00:00:00Z")));
        return this;
    }

    private void revisar() {
        if (fallar) {
            throw new IllegalStateException("BD caida");
        }
    }

    @Override
    public List<TipoNotificacion> listarTipos() {
        revisar();
        return tipos.values().stream().filter(t -> !t.interno()).toList();
    }

    @Override
    public Optional<TipoNotificacion> buscarTipo(final String tipo) {
        revisar();
        return Optional.ofNullable(tipos.get(tipo));
    }

    @Override
    public List<Marcador> marcadores(final String tipo) {
        revisar();
        return marcadores.getOrDefault(tipo, List.of());
    }

    @Override
    public Map<String, List<Marcador>> marcadoresPorTipo() {
        return marcadores;
    }

    @Override
    public List<VersionPlantilla> versiones(final String tipo, final String idioma) {
        return versiones.stream().filter(v -> v.tipo().equals(tipo) && v.idioma().equals(idioma))
                .sorted((a, b) -> Integer.compare(b.version(), a.version())).toList();
    }

    @Override
    public Map<String, List<VersionPlantilla>> versionesPorTipo(final String idioma) {
        final Map<String, List<VersionPlantilla>> r = new LinkedHashMap<>();
        for (final String tipo : tipos.keySet()) {
            r.put(tipo, versiones(tipo, idioma));
        }
        return r;
    }

    @Override
    public Optional<VersionPlantilla> activa(final String tipo, final String idioma) {
        revisar();
        return versiones.stream().filter(v -> v.tipo().equals(tipo) && v.idioma().equals(idioma) && v.activa()).findFirst();
    }

    @Override
    public Optional<VersionPlantilla> version(final String tipo, final String idioma, final int version) {
        revisar();
        return versiones.stream().filter(v -> v.tipo().equals(tipo) && v.idioma().equals(idioma) && v.version() == version).findFirst();
    }

    @Override
    public VersionPlantilla crearYActivar(final String tipo, final String idioma, final String asunto, final String cuerpo, final String creadoPor, final Instant ahora) {
        final int siguiente = versiones(tipo, idioma).stream().mapToInt(VersionPlantilla::version).max().orElse(0) + 1;
        desactivar(tipo, idioma);
        final VersionPlantilla nueva = new VersionPlantilla(UUID.randomUUID(), tipo, idioma, siguiente, asunto, cuerpo, true, creadoPor, ahora);
        versiones.add(nueva);
        return nueva;
    }

    @Override
    public Optional<VersionPlantilla> activar(final String tipo, final String idioma, final int version) {
        final Optional<VersionPlantilla> objetivo = version(tipo, idioma, version);
        if (objetivo.isEmpty()) {
            return Optional.empty();
        }
        desactivar(tipo, idioma);
        versiones.remove(objetivo.get());
        final VersionPlantilla activa = new VersionPlantilla(objetivo.get().id(), tipo, idioma, version, objetivo.get().asunto(),
                objetivo.get().cuerpo(), true, objetivo.get().creadoPor(), objetivo.get().creadoEn());
        versiones.add(activa);
        return Optional.of(activa);
    }

    private void desactivar(final String tipo, final String idioma) {
        final List<VersionPlantilla> copia = new ArrayList<>(versiones);
        versiones.clear();
        for (final VersionPlantilla v : copia) {
            versiones.add(v.tipo().equals(tipo) && v.idioma().equals(idioma)
                    ? new VersionPlantilla(v.id(), v.tipo(), v.idioma(), v.version(), v.asunto(), v.cuerpo(), false, v.creadoPor(), v.creadoEn())
                    : v);
        }
    }
}
