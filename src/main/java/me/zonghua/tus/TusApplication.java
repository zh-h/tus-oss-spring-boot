package me.zonghua.tus;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TusApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(TusApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
    }
}
