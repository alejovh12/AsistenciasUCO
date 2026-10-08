package co.edu.uco.asistenciasuco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LB-008 JPA-02A: la auto-configuracion JPA de Spring Boot es el unico bootstrap JPA (EntityManagerFactory,
 * EntityManager compartido, JpaTransactionManager y entity scanning por el paquete base). Las propiedades
 * de Hibernate especificas de SQL Server viven en {@code SqlServerJpaBootstrapConfiguration}.
 */
@SpringBootApplication(scanBasePackages = "co.edu.uco.asistenciasuco")
public class AsistenciasUcoApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AsistenciasUcoApplication.class, args);
    }
}
