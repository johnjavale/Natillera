package co.natillera.soporte;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * MySQL 8 para las pruebas de integración, compartido por toda la ejecución.
 * Usa Testcontainers cuando hay Docker (CI, Linux, Docker Desktop); si no, un MySQL local (p. ej. el servicio
 * MySQL80 de Windows) configurado con NATILLERA_TEST_DB_URL, NATILLERA_TEST_DB_USER y NATILLERA_TEST_DB_PASSWORD.
 * La base local se borra y se recrea en cada ejecución, por eso su nombre debe terminar en "_test".
 */
public final class MysqlDePrueba {

    public record Conexion(String url, String usuario, String clave, String motor) {
    }

    private static final String URL_LOCAL = "jdbc:mysql://localhost:3306/natillera_test";

    private static Conexion conexion;

    private MysqlDePrueba() {
    }

    public static synchronized Conexion obtener() {
        if (conexion == null) {
            conexion = dockerDisponible() ? conTestcontainers() : local();
        }
        return conexion;
    }

    private static boolean dockerDisponible() {
        if ("local".equalsIgnoreCase(System.getenv("NATILLERA_TEST_DB"))) {
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
        MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("natillera").withUsername("natillera").withPassword("natillera");
        mysql.start();
        return new Conexion(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword(), "Testcontainers mysql:8.0");
    }

    private static Conexion local() {
        String url = Objects.requireNonNullElse(System.getenv("NATILLERA_TEST_DB_URL"), URL_LOCAL);
        String usuario = Objects.requireNonNullElse(System.getenv("NATILLERA_TEST_DB_USER"), "natillera");
        String clave = Objects.requireNonNullElse(System.getenv("NATILLERA_TEST_DB_PASSWORD"), "natillera");
        String separador = url.contains("?") ? "&" : "?";
        try (Connection c = DriverManager.getConnection(url + separador + "createDatabaseIfNotExist=true", usuario, clave);
             Statement st = c.createStatement()) {
            String base = c.getCatalog();
            if (base == null || !base.endsWith("_test")) {
                throw new IllegalStateException("La base de pruebas debe terminar en _test (se borra en cada ejecución): "
                        + base);
            }
            st.execute("DROP DATABASE `" + base + "`");
            st.execute("CREATE DATABASE `" + base + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo preparar el MySQL local de pruebas en " + url
                    + " (revise NATILLERA_TEST_DB_URL, NATILLERA_TEST_DB_USER y NATILLERA_TEST_DB_PASSWORD)", e);
        }
        return new Conexion(url, usuario, clave, "MySQL local");
    }
}
