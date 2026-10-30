package co.natillera.shared.application;

/** Puerto de salida para registrar cambios sensibles (cuotas, tasas, anulaciones, roles, cierre). */
public interface AuditoriaPort {

    void registrar(String entidad, Long entidadId, String accion, String detalle);
}
