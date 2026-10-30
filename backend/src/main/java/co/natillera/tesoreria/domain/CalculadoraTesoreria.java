package co.natillera.tesoreria.domain;

import co.natillera.shared.domain.Dinero;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resúmenes de la hoja "Tesorería": por tesorero y por mes (sección 3.6). */
public final class CalculadoraTesoreria {

    private CalculadoraTesoreria() {
    }

    public record SaldoTesorero(Long tesoreroId, String nombreTesorero, int numeroCajitas, Dinero saldoRegistro,
                                Dinero saldoApp, Dinero diferencia) {
    }

    /** Entradas y salidas agregadas de un mes del ciclo para un tesorero (null = sin tesorero). */
    public record FlujoMes(int mes, Long tesoreroId, Dinero entradas, Dinero salidas) {
    }

    public record ResumenMes(int mes, Dinero entradas, Dinero salidas, Dinero saldoAcumulado,
                             Map<Long, Dinero> saldoAcumuladoPorTesorero) {
    }

    /** saldoTesorero = Σ saldo de sus cajitas. diferencia por tesorero solo si reportó algún saldo. */
    public static List<SaldoTesorero> porTesorero(List<SaldoCajita> cajitas) {
        Map<Long, List<SaldoCajita>> grupos = new LinkedHashMap<>();
        for (SaldoCajita c : cajitas) {
            if (c.tesoreroId() != null) {
                grupos.computeIfAbsent(c.tesoreroId(), k -> new ArrayList<>()).add(c);
            }
        }
        List<SaldoTesorero> resultado = new ArrayList<>();
        grupos.forEach((tesoreroId, lista) -> {
            Dinero registro = Dinero.CERO;
            Dinero app = Dinero.CERO;
            for (SaldoCajita c : lista) {
                registro = registro.sumar(c.saldo());
                if (c.saldoApp() != null) {
                    app = app.sumar(c.saldoApp());
                }
            }
            Dinero diferencia = app.esCero() ? Dinero.CERO : app.restar(registro);
            resultado.add(new SaldoTesorero(tesoreroId, lista.get(0).nombreTesorero(), lista.size(), registro, app,
                    diferencia));
        });
        return resultado;
    }

    /** Entradas, salidas y saldo acumulado (total y por tesorero) al cierre de cada mes. */
    public static List<ResumenMes> porMes(int numeroMeses, List<Long> tesoreros, List<FlujoMes> flujos) {
        List<ResumenMes> meses = new ArrayList<>(numeroMeses);
        Dinero acumulado = Dinero.CERO;
        Map<Long, Dinero> acumuladoTesorero = new LinkedHashMap<>();
        tesoreros.forEach(t -> acumuladoTesorero.put(t, Dinero.CERO));
        // Movimientos anteriores al inicio del ciclo se suman al primer mes
        for (int mes = 1; mes <= numeroMeses; mes++) {
            Dinero entradas = Dinero.CERO;
            Dinero salidas = Dinero.CERO;
            for (FlujoMes f : flujos) {
                boolean delMes = f.mes() == mes || (mes == 1 && f.mes() < 1);
                if (!delMes) {
                    continue;
                }
                entradas = entradas.sumar(f.entradas());
                salidas = salidas.sumar(f.salidas());
                if (f.tesoreroId() != null && acumuladoTesorero.containsKey(f.tesoreroId())) {
                    acumuladoTesorero.merge(f.tesoreroId(), f.entradas().restar(f.salidas()), Dinero::sumar);
                }
            }
            acumulado = acumulado.sumar(entradas).restar(salidas);
            meses.add(new ResumenMes(mes, entradas, salidas, acumulado, new LinkedHashMap<>(acumuladoTesorero)));
        }
        return meses;
    }
}
