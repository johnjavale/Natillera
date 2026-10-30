package co.natillera.auth.application.port.out;

/** Puerto hacia integrantes/tesorería para vincular un usuario con su integrante y su cuenta de tesorero. */
public interface VinculosUsuarioPort {

    /** Crea el tesorero del usuario si aún no existe. @return id del tesorero */
    Long asegurarTesorero(Long usuarioId, String nombre);

    void vincularIntegrante(Long integranteId, Long usuarioId);
}
