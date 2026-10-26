package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityBaselinePdpAdminPropertiesTest {

    private static final Map<InstitutionalRole, String> PERFILES = new EnumMap<>(Map.of(
            InstitutionalRole.ADMINISTRADOR, "a", InstitutionalRole.DECANO, "d", InstitutionalRole.COORDINADOR, "c",
            InstitutionalRole.DOCENTE, "o", InstitutionalRole.ESTUDIANTE, "e"));

    private static SecurityBaselinePdpAdminProperties properties(final String pdp, final String secret,
                                                                  final Map<InstitutionalRole, String> perfiles) {
        return new SecurityBaselinePdpAdminProperties(URI.create(pdp), URI.create("http://localhost:19090/token"),
                "asistencias-provisioner", secret, true, "asistencias-uco", perfiles, null, null);
    }

    @Test
    void sin_secreto_no_esta_configurado_y_el_secreto_nunca_aparece_en_toString() {
        assertFalse(properties("http://localhost:18080", "", PERFILES).configured());
        final SecurityBaselinePdpAdminProperties configurado = properties("http://localhost:18080", "super-secreto", PERFILES);
        assertTrue(configurado.configured());
        assertFalse(configurado.toString().contains("super-secreto"));
    }

    @Test
    void la_bandera_explicita_mantiene_desactivada_la_sincronizacion_aunque_exista_secreto() {
        final SecurityBaselinePdpAdminProperties desactivado = new SecurityBaselinePdpAdminProperties(
                URI.create("http://localhost:18080"), URI.create("http://localhost:19090/token"),
                "asistencias-provisioner", "super-secreto", false, "asistencias-uco", PERFILES, null, null);

        assertFalse(desactivado.configured());
        assertFalse(desactivado.toString().contains("super-secreto"));
    }

    @Test
    void exige_https_salvo_loopback() {
        assertThrows(IllegalStateException.class, () -> properties("http://pdp.ejemplo.org", "s", PERFILES));
        properties("https://pdp.ejemplo.org", "s", PERFILES);
    }

    @Test
    void exige_perfil_para_los_cinco_roles() {
        final Map<InstitutionalRole, String> incompleto = new EnumMap<>(PERFILES);
        incompleto.remove(InstitutionalRole.DECANO);
        assertThrows(IllegalStateException.class, () -> properties("http://localhost:18080", "s", incompleto));
    }

    @Test
    void aplica_timeouts_por_defecto() {
        final SecurityBaselinePdpAdminProperties p = properties("http://localhost:18080", "s", PERFILES);
        assertEquals(Duration.ofSeconds(2), p.connectTimeout());
        assertEquals(Duration.ofSeconds(8), p.requestTimeout());
    }

    /** El mapeo rol→perfil de application.yml debe ser el de la fuente única: el catálogo de acceso del PDP. */
    @Test
    void el_mapeo_rol_perfil_de_application_yml_coincide_con_el_catalogo_de_acceso() throws Exception {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));
        final Properties props = yaml.getObject();
        assertEquals("${SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED:false}",
                props.getProperty("app.providers.security-baseline-pdp-admin.enabled"));
        final JsonNode catalogo = JsonMapper.builder().build()
                .readTree(Files.readString(Path.of("infra/security-baseline/access-catalog.json")));

        final Set<String> perfilesCatalogo = new TreeSet<>();
        for (final JsonNode perfil : catalogo.path("profiles")) {
            final JsonNode roles = perfil.path("roles");
            if (roles.size() == 1) {
                final String rol = roles.get(0).asString();
                final String configurado = props.getProperty("app.providers.security-baseline-pdp-admin.profiles." + rol);
                assertEquals(perfil.path("name").asString(), configurado, "perfil individual del rol " + rol);
                perfilesCatalogo.add(rol);
            }
        }
        final Set<String> todos = new TreeSet<>();
        for (final InstitutionalRole rol : InstitutionalRole.values()) {
            todos.add(rol.name());
        }
        assertEquals(todos, perfilesCatalogo, "cada rol institucional debe tener un perfil individual en el catalogo");
    }
}
