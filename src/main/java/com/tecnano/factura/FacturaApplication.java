package com.tecnano.factura;

import com.tecnano.factura.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FacturaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FacturaApplication.class, args);
    }
}
