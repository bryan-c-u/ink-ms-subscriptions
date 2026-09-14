package com.inklusport.subscriptions.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Los planes semilla viven en init-mysql/10-subscriptions-schema.sql. */
@Component
@Slf4j
public class PlanSeeder implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.debug("Planes iniciales provistos por el esquema SQL; seeder no inserta datos");
    }
}
