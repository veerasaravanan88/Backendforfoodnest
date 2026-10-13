package com.foodnest.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();

        String dbUriStr = System.getenv("DATABASE_URI");
        if (!StringUtils.hasText(dbUriStr)) {
            dbUriStr = System.getenv("DATABASE_URL");
        }

        // If DATABASE_URI or DATABASE_URL is set in postgresql:// or postgres:// format,
        // convert it into standard JDBC format (jdbc:postgresql://host:port/database)
        if (StringUtils.hasText(dbUriStr) && (dbUriStr.startsWith("postgresql://") || dbUriStr.startsWith("postgres://"))) {
            try {
                URI uri = URI.create(dbUriStr);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath(); // e.g. /railway

                if (host != null && path != null && path.length() > 1) {
                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    dataSource.setJdbcUrl(jdbcUrl);
                    log.info("Configured JDBC datasource from DATABASE_URL/DATABASE_URI: jdbc:postgresql://{}:{}{}", host, port, path);

                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        dataSource.setUsername(URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                        dataSource.setPassword(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse DATABASE_URL/DATABASE_URI as URI, relying on standard properties: {}", e.getMessage());
            }
        }

        return dataSource;
    }
}
