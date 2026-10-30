package co.natillera.shared.domain;

import java.util.List;
import java.util.function.Function;

/** Equivalente a RANK(valor; rango) descendente de Excel: los empates comparten puesto. */
public final class Ranking {

    private Ranking() {
    }

    public static <T> int puesto(T elemento, List<T> todos, Function<T, Dinero> criterio) {
        Dinero valor = criterio.apply(elemento);
        int mayores = 0;
        for (T otro : todos) {
            if (criterio.apply(otro).compareTo(valor) > 0) {
                mayores++;
            }
        }
        return mayores + 1;
    }
}
