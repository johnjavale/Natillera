package co.natillera.auth.application;

import co.natillera.auth.application.port.out.AsignacionesPort;
import co.natillera.auth.domain.Alcance;
import co.natillera.auth.domain.Rol;
import co.natillera.shared.domain.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessScopeServiceTest {

    @Mock
    SesionActual sesion;
    @Mock
    AsignacionesPort asignaciones;

    AccessScopeService access;

    static final UsuarioActual ADMIN = new UsuarioActual(1L, "a", "A", Set.of(Rol.ADMIN, Rol.INTEGRANTE), 1L, null, false);
    static final UsuarioActual TESORERO = new UsuarioActual(2L, "t", "T", Set.of(Rol.TESORERO, Rol.INTEGRANTE), 2L, 10L, false);
    static final UsuarioActual INTEGRANTE = new UsuarioActual(3L, "i", "I", Set.of(Rol.INTEGRANTE), 3L, null, false);

    @BeforeEach
    void setUp() {
        access = new AccessScopeService(sesion, asignaciones);
        when(asignaciones.asignacionDe(2L)).thenReturn(Optional.of(new AsignacionesPort.Asignacion(2L, 10L)));
        when(asignaciones.asignacionDe(3L)).thenReturn(Optional.of(new AsignacionesPort.Asignacion(3L, 10L)));
        when(asignaciones.asignacionDe(7L)).thenReturn(Optional.of(new AsignacionesPort.Asignacion(7L, 20L)));
        when(asignaciones.asignacionDe(99L)).thenReturn(Optional.empty());
        when(asignaciones.integrantesDeTesorero(10L)).thenReturn(List.of(2L, 3L));
    }

    @Test
    void alcancesPorRol() {
        when(sesion.usuario()).thenReturn(ADMIN);
        assertThat(access.alcance()).isEqualTo(Alcance.ALL);
        assertThat(access.integrantesVisibles()).isEmpty();
        when(sesion.usuario()).thenReturn(TESORERO);
        assertThat(access.alcance()).isEqualTo(Alcance.ASSIGNED);
        assertThat(access.integrantesVisibles()).contains(Set.of(2L, 3L));
        when(sesion.usuario()).thenReturn(INTEGRANTE);
        assertThat(access.alcance()).isEqualTo(Alcance.OWN);
        assertThat(access.integrantesVisibles()).contains(Set.of(3L));
    }

    @Test
    void integranteFueraDeAlcanceEs404() {
        when(sesion.usuario()).thenReturn(INTEGRANTE);
        access.exigirVerIntegrante(3L);
        assertThatThrownBy(() -> access.exigirVerIntegrante(7L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void adminRecibe404SoloSiNoExiste() {
        when(sesion.usuario()).thenReturn(ADMIN);
        access.exigirVerIntegrante(7L);
        assertThatThrownBy(() -> access.exigirVerIntegrante(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void tesoreroGestionaSoloSusAsignados() {
        when(sesion.usuario()).thenReturn(TESORERO);
        access.exigirGestionarIntegrante(3L);
        assertThatThrownBy(() -> access.exigirGestionarIntegrante(7L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void integranteVeSuCajitaPeroNoLaAdministra() {
        when(sesion.usuario()).thenReturn(INTEGRANTE);
        assertThatThrownBy(() -> access.exigirGestionarIntegrante(3L)).isInstanceOf(AccessDeniedException.class);
    }
}
