package co.natillera.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit de login en memoria (ventana deslizante): N intentos fallidos por IP y por usuario en la ventana.
 * Suficiente para una sola instancia; no usa tareas programadas (compatible con Railway app sleeping).
 */
@Component
public class LimitadorIntentosLogin {

    private final int maxIntentos;
    private final Duration ventana;
    private final Clock clock;
    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public LimitadorIntentosLogin(@Value("${app.login.max-intentos:5}") int maxIntentos,
                                  @Value("${app.login.ventana-minutos:15}") int ventanaMinutos, Clock clock) {
        this.maxIntentos = maxIntentos;
        this.ventana = Duration.ofMinutes(ventanaMinutos);
        this.clock = clock;
    }

    public void verificar(String ip, String login) {
        Instant ahora = clock.instant();
        long espera = Math.max(esperaPara(claveIp(ip), ahora), esperaPara(claveUsuario(login), ahora));
        if (espera > 0) {
            throw new DemasiadosIntentosException(espera);
        }
    }

    public void registrarFallo(String ip, String login) {
        Instant ahora = clock.instant();
        registrar(claveIp(ip), ahora);
        registrar(claveUsuario(login), ahora);
        if (fallos.size() > 10_000) {
            fallos.entrySet().removeIf(e -> limpiar(e.getValue(), ahora).isEmpty());
        }
    }

    public void registrarExito(String login) {
        fallos.remove(claveUsuario(login));
    }

    public void reiniciar() {
        fallos.clear();
    }

    private long esperaPara(String clave, Instant ahora) {
        Deque<Instant> cola = fallos.get(clave);
        if (cola == null) {
            return 0;
        }
        synchronized (cola) {
            limpiar(cola, ahora);
            if (cola.size() < maxIntentos) {
                return 0;
            }
            return Math.max(1, Duration.between(ahora, cola.peekFirst().plus(ventana)).toSeconds());
        }
    }

    private void registrar(String clave, Instant ahora) {
        Deque<Instant> cola = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            limpiar(cola, ahora);
            cola.addLast(ahora);
        }
    }

    private Deque<Instant> limpiar(Deque<Instant> cola, Instant ahora) {
        while (!cola.isEmpty() && !cola.peekFirst().plus(ventana).isAfter(ahora)) {
            cola.pollFirst();
        }
        return cola;
    }

    private static String claveIp(String ip) {
        return "ip:" + (ip == null ? "desconocida" : ip);
    }

    private static String claveUsuario(String login) {
        return "u:" + (login == null ? "" : login.trim().toLowerCase(Locale.ROOT));
    }
}
