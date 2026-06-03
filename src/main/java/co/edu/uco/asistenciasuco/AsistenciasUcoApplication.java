package co.edu.uco.asistenciasuco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "co.edu.uco.asistenciasuco")
public class AsistenciasUcoApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AsistenciasUcoApplication.class, args);
    }
}
