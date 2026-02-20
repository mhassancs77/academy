package com.academy.config;


import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary  // ← ADD THIS
    public DataSource dataSource() {
        String pgHost = System.getenv("PGHOST");
        String pgPort = System.getenv("PGPORT");
        String pgDb = System.getenv("PGDATABASE");
        String pgUser = System.getenv("PGUSER");
        String pgPass = System.getenv("PGPASSWORD");

        String url = String.format("jdbc:postgresql://%s:%s/%s", pgHost, pgPort, pgDb);
        System.out.println("✅ Connecting to Postgres: " + pgHost);

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(pgUser)
                .password(pgPass)
                .build();
    }
}