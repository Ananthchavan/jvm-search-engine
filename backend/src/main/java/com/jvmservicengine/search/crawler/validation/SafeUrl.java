package com.jvmservicengine.search.crawler.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a URL passes {@link UrlSafetyValidator#isSafe}.
 * Only public http/https URLs with no userinfo, allowed ports,
 * and no private/reserved IP addresses are accepted.
 */
@Documented
@Constraint(validatedBy = SafeUrlConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeUrl {
    String message() default "URL must be a public http/https address (no private IPs, no userinfo, ports 80/443 only)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

