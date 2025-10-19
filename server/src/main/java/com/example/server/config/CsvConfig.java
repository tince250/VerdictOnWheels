package com.example.server.config;

import com.example.server.repository.CsvConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
public class CsvConfig {

    @Bean
    public CsvConnector csvConnector(@Value("${csv.path}") String path) {
        return new CsvConnector(Paths.get(path));
    }

}
