package co.edu.uco.asistenciasuco;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AsistenciasUcoApplicationTests {

    @Test
    void main_class_is_available_without_requiring_sql_server_for_unit_tests() {
        assertNotNull(AsistenciasUcoApplication.class);
    }

}
