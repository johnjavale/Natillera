package co.natillera.shared.infrastructure.persistence;

import co.natillera.shared.domain.Dinero;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Conversión Dinero ↔ BigDecimal usada por los mappers de MapStruct (persistencia y DTOs). */
@Component
public class DineroMapeo {

    public Dinero aDinero(BigDecimal valor) {
        return valor == null ? null : Dinero.de(valor);
    }

    public BigDecimal aBigDecimal(Dinero dinero) {
        return dinero == null ? null : dinero.aCentavos();
    }
}
