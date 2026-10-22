package com.conectatech.sgs_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RutChilenoValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RutChileno {
    String message() default "El RUN ingresado no es válido (formato o dígito verificador incorrecto)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
