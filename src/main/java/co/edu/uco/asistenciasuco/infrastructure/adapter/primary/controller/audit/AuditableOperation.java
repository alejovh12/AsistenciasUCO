package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableOperation {

    String action();

    String resourceType();

    String resourceIdPathVariable() default "";

    String resourceIdRequestField() default "";

    String resourceIdResponseField() default "";
}
