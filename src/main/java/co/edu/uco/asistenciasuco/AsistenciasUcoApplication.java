package co.edu.uco.asistenciasuco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * La auto-configuracion JPA de Boot se excluye a proposito (LB-002.1): el EntityManagerFactory de la
 * query piloto de asistencia se construye en el Composition Root solo con
 * {@code app.adapters.persistence.asistencia-query-provider=jpa}. Asi el default jdbc no crea
 * Hibernate ni requiere SQL Server para arrancar, y no se registra un JpaTransactionManager.
 */
@SpringBootApplication(
        scanBasePackages = "co.edu.uco.asistenciasuco",
        excludeName = {
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.hibernate.autoconfigure.metrics.HibernateMetricsAutoConfiguration",
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
        }
)
public class AsistenciasUcoApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AsistenciasUcoApplication.class, args);
    }
}
