package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.binding;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public final class StrictHttpBindingAdvice {

    @InitBinder
    public void registerStrictBooleanEditors(final WebDataBinder binder) {
        binder.registerCustomEditor(Boolean.class, new StrictBooleanPropertyEditor());
        binder.registerCustomEditor(boolean.class, new StrictBooleanPropertyEditor());
    }
}
