package co.edu.uco.asistenciasuco.architecture;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code InstitutionalRole} es un enum puro del dominio: solo los 5 roles institucionales,
 * sin métodos propios ni campos propios.
 */
class InstitutionalRolePurityTest {

    @Test
    void institutional_role_no_declara_metodos_propios() {
        final Set<String> implicitEnumMethodNames = Set.of("values", "valueOf");

        final List<Method> customMethods = Arrays.stream(InstitutionalRole.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .filter(method -> !implicitEnumMethodNames.contains(method.getName()))
                .toList();

        assertTrue(
                customMethods.isEmpty(),
                "InstitutionalRole no debe declarar métodos propios (p.ej. authority(), " +
                        "fromTokenRole(), databaseCode()); encontrados: " + customMethods
        );
    }

    @Test
    void institutional_role_no_declara_campos_propios() {
        // Los enums Java sin campos propios no declaran ningun java.lang.reflect.Field
        // adicional a los constantes; un campo como el antiguo "databaseCode" apareceria aqui.
        final long nonConstantFieldCount = Arrays.stream(InstitutionalRole.class.getDeclaredFields())
                .filter(field -> !field.isEnumConstant())
                .filter(field -> !field.isSynthetic())
                .count();

        assertEquals(0, nonConstantFieldCount, "InstitutionalRole no debe tener campos propios.");
    }

    @Test
    void institutional_role_contiene_exactamente_los_cinco_roles_institucionales() {
        final Set<String> expected = Set.of("ADMINISTRADOR", "DECANO", "COORDINADOR", "DOCENTE", "ESTUDIANTE");

        final Set<String> actual = Arrays.stream(InstitutionalRole.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(expected, actual);
    }
}
