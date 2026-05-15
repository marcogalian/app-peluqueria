package com.marcog.peluqueria.chat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaMensajeInternoRepository extends JpaRepository<MensajeInternoEntity, UUID> {

    @Query("""
            select m
            from MensajeInternoEntity m
            where (
                    (m.emisorUserId = :usuarioId and m.receptorUserId = :contactoUserId)
                 or (m.emisorUserId = :contactoUserId and m.receptorUserId = :usuarioId)
                  )
              and m.archivado = false
            order by m.enviadoEn asc
            """)
    List<MensajeInternoEntity> findConversacion(
            @Param("usuarioId") UUID usuarioId,
            @Param("contactoUserId") UUID contactoUserId
    );

    long countByReceptorUserIdAndLeidoFalseAndArchivadoFalse(UUID receptorUserId);

    @Modifying
    @Query("""
            update MensajeInternoEntity m
            set m.leido = true
            where m.receptorUserId = :usuarioId
              and m.leido = false
              and m.archivado = false
            """)
    int marcarRecibidosComoLeidos(@Param("usuarioId") UUID usuarioId);
}
