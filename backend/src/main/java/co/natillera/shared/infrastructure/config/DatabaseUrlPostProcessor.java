package co.natillera.shared.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Railway expone DATABASE_URL como {@code postgresql://usuario:clave@host:puerto/base}. Si llega en ese formato,
 * se traduce a las propiedades JDBC de Spring. Si ya es {@code jdbc:postgresql://...}, se usa tal cual.
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String url = env.getProperty("DATABASE_URL");
        if (url == null || !(url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            return;
        }
        Map<String, Object> props = new HashMap<>(convertir(url));
        env.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrl", props));
    }

    static Map<String, String> convertir(String url) {
        URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
        Map<String, String> props = new HashMap<>();
        int puerto = uri.getPort() == -1 ? 5432 : uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        props.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + puerto + uri.getRawPath() + query);
        if (uri.getRawUserInfo() != null) {
            String[] partes = uri.getRawUserInfo().split(":", 2);
            props.put("spring.datasource.username", URLDecoder.decode(partes[0], StandardCharsets.UTF_8));
            if (partes.length > 1) {
                props.put("spring.datasource.password", URLDecoder.decode(partes[1], StandardCharsets.UTF_8));
            }
        }
        return props;
    }
}
