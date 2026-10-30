package co.natillera.soporte;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * PostgreSQL 16 para las pruebas de integración, compartido por toda la ejecución.
 * Usa Testcontainers cuando hay Docker (CI, Linux, Docker Desktop); si no, un PostgreSQL 16 embebido
 * (binarios de io.zonky) para poder correr las pruebas en máquinas sin Docker.
 */
public final class PostgresDePrueba {

    public record Conexion(String url, String usuario, String clave, String motor) {
    }

    private static Conexion conexion;

    private PostgresDePrueba() {
    }

    public static synchronized Conexion obtener() {
        if (conexion == null) {
            conexion = dockerDisponible() ? conTestcontainers() : embebido();
        }
        return conexion;
    }

    private static boolean dockerDisponible() {
        if ("embebido".equalsIgnoreCase(System.getenv("NATILLERA_TEST_DB"))) {
            return false;
        }
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    @SuppressWarnings("resource")
    private static Conexion conTestcontainers() {
        PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("natillera").withUsername("natillera").withPassword("natillera");
        pg.start();
        return new Conexion(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword(), "Testcontainers postgres:16-alpine");
    }

    private static Conexion embebido() {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    pg.close();
                } catch (IOException ignored) {
                    // apagado best-effort
                }
            }));
            return new Conexion(pg.getJdbcUrl("postgres", "postgres"), "postgres", "postgres",
                    "PostgreSQL 16 embebido (zonky)");
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo iniciar PostgreSQL embebido", e);
        }
    }
}
