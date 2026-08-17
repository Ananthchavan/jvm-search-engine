package com.jvmservicengine.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // empty as this simply tells spring boot
    // to turn on the background task scheduler
}
