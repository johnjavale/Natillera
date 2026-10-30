package co.natillera.integrantes.application.port.out;

import co.natillera.integrantes.domain.Integrante;

import java.util.List;
import java.util.Optional;

public interface IntegranteRepositoryPort {

    List<Integrante> listar();

    Optional<Integrante> buscar(Long id);

    Integrante guardar(Integrante integrante);

    int contarActivos();
}
