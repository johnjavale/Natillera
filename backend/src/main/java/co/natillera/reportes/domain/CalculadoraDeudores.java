package co.natillera.reportes.domain;

import co.natillera.shared.domain.Dinero;

import java.util.Comparator;
import java.util.List;

/** "¿Quién debe y cuánto?": deuda por atraso de aportes, préstamos y boletas sin pagar. */
public final class CalculadoraDeudores {

    private CalculadoraDeudores() {
    }

    public enum TipoDeuda { APORTES, PRESTAMOS, BOLETAS }

    public record Deudor(Long integranteId, String nombre, Long tesoreroId, String nombreTesorero,
                         Dinero atrasoAportes, Dinero saldoPrestamos, Dinero vencidoPrestamos,
                         Dinero boletasPendientes) {

        public Dinero total() {
            return atrasoAportes.sumar(saldoPrestamos).sumar(boletasPendientes);
        }

        public boolean enMora() {
            return atrasoAportes.esPositivo() || vencidoPrestamos.esPositivo();
        }

        public Dinero deudaDe(TipoDeuda tipo) {
            return switch (tipo) {
                case APORTES -> atrasoAportes;
                case PRESTAMOS -> saldoPrestamos;
                case BOLETAS -> boletasPendientes;
            };
        }
    }

    /** Filtra los que deben (total > 0, o > 0 en el tipo pedido) y ordena por total descendente. */
    public static List<Deudor> filtrarYOrdenar(List<Deudor> todos, Long tesoreroId, TipoDeuda tipo,
                                               boolean soloMora) {
        return todos.stream()
                .filter(d -> tesoreroId == null || tesoreroId.equals(d.tesoreroId()))
                .filter(d -> tipo == null ? d.total().esPositivo() : d.deudaDe(tipo).esPositivo())
                .filter(d -> !soloMora || d.enMora())
                .sorted(Comparator.comparing(Deudor::total).reversed().thenComparing(Deudor::nombre))
                .toList();
    }
}
