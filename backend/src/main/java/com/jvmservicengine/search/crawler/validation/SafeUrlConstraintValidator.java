package com.jvmservicengine.search.crawler.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * Bridges the Bean Validation {@link SafeUrl} annotation to
 * {@link UrlSafetyValidator}. Spring injects the validator as a bean so
 * the full SSRF check (DNS resolution, port allowlist, etc.) runs at
 * validation time.
 */
@Component
public class SafeUrlConstraintValidator implements ConstraintValidator<SafeUrl, String> {

    private UrlSafetyValidator urlSafetyValidator;

    public SafeUrlConstraintValidator() {
    }

    public SafeUrlConstraintValidator(UrlSafetyValidator urlSafetyValidator) {
        this.urlSafetyValidator = urlSafetyValidator;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setUrlSafetyValidator(UrlSafetyValidator urlSafetyValidator) {
        this.urlSafetyValidator = urlSafetyValidator;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // Let @NotBlank handle the null/blank case
            return true;
        }
        if (urlSafetyValidator == null) {
            return true;
        }
        return urlSafetyValidator.isSafe(value);
    }
}

