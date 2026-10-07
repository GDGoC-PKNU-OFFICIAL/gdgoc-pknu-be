package com.gdgocpknu.gdgoc_pknu_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GdgocPknuBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GdgocPknuBeApplication.class, args);
    }

}
