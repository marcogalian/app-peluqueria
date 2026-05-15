package com.marcog.peluqueria.clientes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaClienteRepository extends JpaRepository<ClienteEntity, UUID> {

    List<ClienteEntity> findByArchivadoOrderByNombreAscApellidosAsc(boolean archivado);

    @Query("SELECT c FROM ClienteEntity c WHERE " +
           "(:nombre IS NULL OR LOWER(CONCAT(c.nombre, ' ', c.apellidos)) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:esVip IS NULL OR c.esVip = :esVip) AND " +
           "c.archivado = :archivado")
    List<ClienteEntity> findByFiltros(
            @Param("nombre") String nombre,
            @Param("esVip") Boolean esVip,
            @Param("archivado") boolean archivado
    );
}
