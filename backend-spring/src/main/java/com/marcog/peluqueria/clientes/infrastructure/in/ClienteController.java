package com.marcog.peluqueria.clientes.infrastructure.in;

import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.port.in.RegistrarClienteUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.marcog.peluqueria.clientes.application.actualizar.ActualizarClienteService;
import com.marcog.peluqueria.clientes.application.consultar.ConsultarClienteService;
import com.marcog.peluqueria.clientes.domain.model.HistorialClinicoDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Clientes", description = "Gestion de clientes y historial clinico")
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final RegistrarClienteUseCase registrarClienteUseCase;
    private final ConsultarClienteService consultarClienteService;
    private final ActualizarClienteService actualizarClienteService;

    @Operation(summary = "Listar clientes", description = "Retorna clientes con filtros opcionales por nombre, VIP y archivado")
    @ApiResponse(responseCode = "200", description = "Lista de clientes")
    @GetMapping
    public ResponseEntity<List<Cliente>> getAllClientes(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean esVip,
            @RequestParam(defaultValue = "false") boolean archivado) {
        if (nombre != null || esVip != null) {
            return ResponseEntity.ok(consultarClienteService.buscarClientes(nombre, esVip, archivado));
        }
        return ResponseEntity.ok(consultarClienteService.getAllClientes(archivado));
    }

    @Operation(summary = "Historial clinico", description = "Obtiene el historial clinico completo de un cliente")
    @ApiResponse(responseCode = "200", description = "Historial encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    @GetMapping("/{id}/historial")
    public ResponseEntity<HistorialClinicoDTO> getHistorialClinico(@PathVariable UUID id) {
        return ResponseEntity.ok(consultarClienteService.getHistorialClinico(id));
    }

    @Operation(summary = "Registrar cliente", description = "Crea un nuevo cliente en el sistema")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @PostMapping
    public ResponseEntity<Cliente> registrar(@RequestBody Cliente cliente) {
        Cliente clienteCreado = registrarClienteUseCase.ejecutar(cliente);
        return new ResponseEntity<>(clienteCreado, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar cliente", description = "Modifica los datos de un cliente existente")
    @ApiResponse(responseCode = "200", description = "Cliente actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable UUID id, @RequestBody Cliente cliente) {
        return ResponseEntity.ok(actualizarClienteService.actualizarCliente(id, cliente));
    }

    @Operation(summary = "Archivar cliente", description = "Marca un cliente como archivado (soft delete)")
    @ApiResponse(responseCode = "200", description = "Cliente archivado")
    @PostMapping("/{id}/archivar")
    public ResponseEntity<Cliente> archivar(@PathVariable UUID id) {
        return ResponseEntity.ok(actualizarClienteService.actualizarArchivado(id, true));
    }

    @Operation(summary = "Reactivar cliente", description = "Reactiva un cliente archivado")
    @ApiResponse(responseCode = "200", description = "Cliente reactivado")
    @PostMapping("/{id}/reactivar")
    public ResponseEntity<Cliente> reactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(actualizarClienteService.actualizarArchivado(id, false));
    }

    @Operation(summary = "Actualizar consentimiento fotos", description = "Actualiza el consentimiento de fotos del cliente")
    @ApiResponse(responseCode = "200", description = "Consentimiento actualizado")
    @PostMapping("/{id}/consentimiento")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Cliente> actualizarConsentimiento(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Boolean> body) {
        boolean valor = Boolean.TRUE.equals(body.get("consentimientoFotos"));
        return ResponseEntity.ok(actualizarClienteService.actualizarConsentimiento(id, valor));
    }
}
