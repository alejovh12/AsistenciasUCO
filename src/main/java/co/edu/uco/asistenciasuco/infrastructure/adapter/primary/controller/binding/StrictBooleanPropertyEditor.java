package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.binding;

import java.beans.PropertyEditorSupport;

public final class StrictBooleanPropertyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (text == null) {
            setValue(null);
            return;
        }
        if ("true".equals(text)) {
            setValue(Boolean.TRUE);
            return;
        }
        if ("false".equals(text)) {
            setValue(Boolean.FALSE);
            return;
        }
        throw new IllegalArgumentException("Invalid strict boolean HTTP value.");
    }
}
