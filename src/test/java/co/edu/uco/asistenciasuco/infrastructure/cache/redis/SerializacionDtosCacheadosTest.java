package co.edu.uco.asistenciasuco.infrastructure.cache.redis;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.PlanEstudioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * REDIS-001: cada resultado que la cache Redis guarda como JSON debe volver identico. Se construye una instancia con todos
 * sus campos llenos (por reflexion, para que un campo nuevo del DTO tambien quede cubierto), se escribe, se lee con el
 * mismo {@link TypeReference} que usa el decorador y se compara el JSON resultante.
 */
class SerializacionDtosCacheadosTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void portal_del_estudiante_y_del_docente() {
        idaYVuelta(List.of(muestra(MateriaEstudianteDTO.class)), new TypeReference<List<MateriaEstudianteDTO>>() { });
        idaYVuelta(List.of(muestra(HorarioEstudianteDTO.class)), new TypeReference<List<HorarioEstudianteDTO>>() { });
        idaYVuelta(muestra(ResumenMateriasEstudianteDTO.class), new TypeReference<ResumenMateriasEstudianteDTO>() { });
        idaYVuelta(List.of(muestra(PlanEstudioEstudianteDTO.class)), new TypeReference<List<PlanEstudioEstudianteDTO>>() { });
        idaYVuelta(List.of(muestra(AsignaturaDocenteDTO.class)), new TypeReference<List<AsignaturaDocenteDTO>>() { });
        idaYVuelta(List.of(muestra(HorarioDocenteDTO.class)), new TypeReference<List<HorarioDocenteDTO>>() { });
    }

    @Test
    void indicadores() {
        idaYVuelta(muestra(KpisCoordinadorDTO.class), new TypeReference<KpisCoordinadorDTO>() { });
        idaYVuelta(muestra(AlertasPaginaDTO.class), new TypeReference<AlertasPaginaDTO>() { });
        idaYVuelta(muestra(KpisDecanoDTO.class), new TypeReference<KpisDecanoDTO>() { });
        idaYVuelta(List.of(muestra(ComparativaProgramaDTO.class)), new TypeReference<List<ComparativaProgramaDTO>>() { });
        idaYVuelta(muestra(AforoGruposPaginaDTO.class), new TypeReference<AforoGruposPaginaDTO>() { });
        idaYVuelta(muestra(KpisInstitucionDTO.class), new TypeReference<KpisInstitucionDTO>() { });
        idaYVuelta(List.of(muestra(ComparativaFacultadDTO.class)), new TypeReference<List<ComparativaFacultadDTO>>() { });
        idaYVuelta(List.of(muestra(PuntoTendenciaDTO.class)), new TypeReference<List<PuntoTendenciaDTO>>() { });
    }

    @Test
    void catalogos() {
        idaYVuelta(List.of(muestra(CatalogoProgramaDTO.class)), new TypeReference<List<CatalogoProgramaDTO>>() { });
        idaYVuelta(List.of(new TipoIdentificacionDTO(UUID.randomUUID(), "CC", "Cedula")),
                new TypeReference<List<TipoIdentificacionDTO>>() { });
    }

    private <T> void idaYVuelta(final T original, final TypeReference<T> tipo) {
        final String json = mapper.writeValueAsString(original);
        final T leido = mapper.readValue(json, tipo);
        assertEquals(json, mapper.writeValueAsString(leido), "ida y vuelta de " + tipo.getType());
        if (original instanceof Record) {
            assertEquals(original, leido);
        }
    }

    private static <T> T muestra(final Class<T> tipo) {
        return tipo.cast(valor(tipo, 0));
    }

    private static Object valor(final Type tipo, final int profundidad) {
        if (tipo instanceof ParameterizedType parametrizado) {
            final Class<?> crudo = (Class<?>) parametrizado.getRawType();
            final Type[] argumentos = parametrizado.getActualTypeArguments();
            if (List.class.isAssignableFrom(crudo)) {
                final List<Object> lista = new ArrayList<>();
                lista.add(valor(argumentos[0], profundidad + 1));
                return lista;
            }
            if (Set.class.isAssignableFrom(crudo)) {
                final Set<Object> conjunto = new LinkedHashSet<>();
                conjunto.add(valor(argumentos[0], profundidad + 1));
                return conjunto;
            }
            if (Map.class.isAssignableFrom(crudo)) {
                final Map<Object, Object> mapa = new LinkedHashMap<>();
                mapa.put(valor(argumentos[0], profundidad + 1), valor(argumentos[1], profundidad + 1));
                return mapa;
            }
            throw new IllegalArgumentException("Tipo generico no soportado por la muestra: " + tipo);
        }
        final Class<?> clase = (Class<?>) tipo;
        if (clase == String.class) {
            return "texto";
        }
        if (clase == UUID.class) {
            return UUID.fromString("22222222-2222-2222-2222-222222222222");
        }
        if (clase == Integer.class || clase == int.class) {
            return 7;
        }
        if (clase == Long.class || clase == long.class) {
            return 9L;
        }
        if (clase == Double.class || clase == double.class) {
            return 2.5d;
        }
        if (clase == Float.class || clase == float.class) {
            return 1.5f;
        }
        if (clase == Short.class || clase == short.class) {
            return (short) 3;
        }
        if (clase == Boolean.class || clase == boolean.class) {
            return Boolean.TRUE;
        }
        if (clase == BigDecimal.class) {
            return new BigDecimal("12.75");
        }
        if (clase == LocalDate.class) {
            return LocalDate.of(2026, 11, 25);
        }
        if (clase == LocalTime.class) {
            return LocalTime.of(7, 30);
        }
        if (clase == LocalDateTime.class) {
            return LocalDateTime.of(2026, 11, 25, 7, 30);
        }
        if (clase == Instant.class) {
            return Instant.parse("2026-11-25T12:30:00Z");
        }
        if (clase == OffsetDateTime.class) {
            return OffsetDateTime.of(2026, 11, 25, 7, 30, 0, 0, ZoneOffset.ofHours(-5));
        }
        if (clase.isEnum()) {
            return clase.getEnumConstants()[0];
        }
        if (clase.isRecord() && profundidad < 5) {
            final RecordComponent[] componentes = clase.getRecordComponents();
            final Object[] argumentos = new Object[componentes.length];
            final Class<?>[] tipos = new Class<?>[componentes.length];
            for (int i = 0; i < componentes.length; i++) {
                argumentos[i] = valor(componentes[i].getGenericType(), profundidad + 1);
                tipos[i] = componentes[i].getType();
            }
            try {
                final Constructor<?> constructor = clase.getDeclaredConstructor(tipos);
                constructor.setAccessible(true);
                return constructor.newInstance(argumentos);
            } catch (final ReflectiveOperationException exception) {
                throw new IllegalStateException("No se pudo construir la muestra de " + clase, exception);
            }
        }
        throw new IllegalArgumentException("Tipo no soportado por la muestra: " + clase);
    }
}
