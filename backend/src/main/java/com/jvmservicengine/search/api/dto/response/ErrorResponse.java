package com.jvmservicengine.search.api.dto.response;

import java.time.LocalDateTime;


public record ErrorResponse (
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        String path
) {}