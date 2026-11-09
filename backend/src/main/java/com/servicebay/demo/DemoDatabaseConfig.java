package com.servicebay.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Method;

/**
 * "demo" profile: starts an embedded PostgreSQL (data kept in ~/.servicebay-demo/pgdata), so the app runs with only Java.
 * Uses reflection so the normal build does not need the embedded-postgres library (it is only in the -Pdemo build).
 */
@Slf4j
@Configuration
@Profile("demo")
public class DemoDatabaseConfig {

    @Bean
    public DataSource dataSource() throws Exception {
        File dataDir = new File(System.getProperty("user.home"), ".servicebay-demo/pgdata");
        dataDir.mkdirs();
        Class<?> ep = Class.forName("io.zonky.test.db.postgres.embedded.EmbeddedPostgres");
        Object builder = ep.getMethod("builder").invoke(null);
        Class<?> b = builder.getClass();
        b.getMethod("setPort", int.class).invoke(builder, 54329);
        b.getMethod("setDataDirectory", File.class).invoke(builder, dataDir);
        b.getMethod("setCleanDataDirectory", boolean.class).invoke(builder, false);
        b.getMethod("setServerConfig", String.class, String.class).invoke(builder, "timezone", "UTC");
        log.info("Starting embedded PostgreSQL (data: {}) ...", dataDir);
        Object pg = b.getMethod("start").invoke(builder);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { ep.getMethod("close").invoke(pg); } catch (Exception ignored) { }
        }));
        Method m = ep.getMethod("getPostgresDatabase");
        return (DataSource) m.invoke(pg);
    }
}
