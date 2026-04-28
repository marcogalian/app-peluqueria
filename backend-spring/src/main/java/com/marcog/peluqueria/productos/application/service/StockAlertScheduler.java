package com.marcog.peluqueria.productos.application.service;

import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertScheduler {

    private final ProductoRepositoryPort repository;
    private final NotificationService notificationService;

    // Cada día a las 8:00
    @Scheduled(cron = "0 0 8 * * *")
    public void alertarStockBajo() {
        log.info("Revisando stock de productos...");

        List<Producto> bajoStock = repository.findAll().stream()
                .filter(p -> p.isActivo()
                        && p.getStockMinimo() != null
                        && p.getStock() != null
                        && p.getStock() <= p.getStockMinimo())
                .collect(Collectors.toList());

        if (bajoStock.isEmpty()) {
            log.info("Sin productos con stock bajo hoy.");
            return;
        }

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Buenos días,\n\n");
        cuerpo.append("Los siguientes productos tienen stock bajo:\n\n");
        for (Producto p : bajoStock) {
            cuerpo.append("• ").append(p.getNombre())
                  .append(" — Stock: ").append(p.getStock())
                  .append(" / Mínimo: ").append(p.getStockMinimo())
                  .append("\n");
        }
        cuerpo.append("\nPor favor, contacta con tus proveedores.\n\nPeluquería Isabella");

        notificationService.notificarAdmin(
                "📦 Resumen diario: " + bajoStock.size() + " producto(s) con stock bajo",
                cuerpo.toString()
        );

        log.info("Alerta de stock enviada. {} productos afectados.", bajoStock.size());
    }
}
