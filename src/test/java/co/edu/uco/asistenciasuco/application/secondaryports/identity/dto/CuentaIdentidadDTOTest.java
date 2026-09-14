package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El resultado de Identity que cruza Infrastructure -&gt; Application debe ser neutral: ni
 * mensajes humanos del IdP ni datos redundantes. Esta prueba fija ese contrato mínimo por
 * reflexión sobre los componentes del record, para que un cambio futuro que reintroduzca
 * {@code username}/{@code mensaje} falle explícitamente aquí.
 */
class CuentaIdentidadDTOTest {

    @Test
    void solo_expone_idExterno_y_newlyCreated() {
        final List<String> nombresComponentes = Arrays.stream(CuentaIdentidadDTO.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(2, nombresComponentes.size());
        assertTrue(nombresComponentes.contains("idExterno"));
        assertTrue(nombresComponentes.contains("newlyCreated"));
    }

    @Test
    void no_expone_username_ni_mensaje() {
        final List<String> nombresComponentes = Arrays.stream(CuentaIdentidadDTO.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertTrue(nombresComponentes.stream().noneMatch(nombre -> nombre.equals("username")));
        assertTrue(nombresComponentes.stream().noneMatch(nombre -> nombre.equals("mensaje")));
    }
}
