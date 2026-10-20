package com.drmayday.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;

@SpringBootApplication(scanBasePackages = "com.drmayday")
@EntityScan(basePackages = "com.drmayday")
@EnableJpaRepositories(basePackages = "com.drmayday")
public class DrMayDayApplication {

    static {
        loadDotenv();
    }

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(DrMayDayApplication.class, args);
    }

    public static void loadDotenv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("../.env");
        }
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String val = line.substring(idx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String dbUrl = System.getProperty("DB_URL");
        if (dbUrl == null) {
            dbUrl = System.getenv("DB_URL");
        }
        if (dbUrl != null && !dbUrl.isEmpty()) {
            if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(dbUrl.replace("postgres://", "postgresql://"));
                    String userInfo = uri.getUserInfo();
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    String query = uri.getQuery();
                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null ? "?" + query : "");

                    System.setProperty("SPRING_DATASOURCE_URL", jdbcUrl);
                    System.setProperty("spring.datasource.url", jdbcUrl);

                    if (userInfo != null) {
                        String[] parts = userInfo.split(":", 2);
                        System.setProperty("SPRING_DATASOURCE_USERNAME", parts[0]);
                        System.setProperty("spring.datasource.username", parts[0]);
                        if (parts.length > 1) {
                            System.setProperty("SPRING_DATASOURCE_PASSWORD", parts[1]);
                            System.setProperty("spring.datasource.password", parts[1]);
                        }
                    }
                } catch (Exception ignored) {
                }
            } else if (dbUrl.startsWith("jdbc:postgresql://")) {
                System.setProperty("SPRING_DATASOURCE_URL", dbUrl);
                System.setProperty("spring.datasource.url", dbUrl);
            }
        }
    }
}
