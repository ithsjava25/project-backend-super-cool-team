package org.example.cyberwatch;

import org.springframework.boot.SpringApplication;

public class TestCyberWatchApplication {

    public static void main(String[] args) {
        SpringApplication.from(CyberWatchApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
