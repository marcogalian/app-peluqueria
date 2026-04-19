package com.marcog.peluqueria.clientes.infrastructure.in;

import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.port.in.RegistrarClienteUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.marcog.peluqueria.clientes.application.actualizar.ActualizarClienteService;
import com.marcog.peluqueria.clientes.application.consultar.ConsultarClienteService;
import com.marcog.peluqueria.clientes.domain.model.HistorialClinicoDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final RegistrarClienteUseCase registrarClienteUseCase;
    private final ConsultarClienteService consultarClienteService;
    private final ActualizarClienteService actualizarClienteService;

    @GetMapping
    public ResponseEntity<List<Cliente>> getAllClientes(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean esVip) {
        if (nombre != null || esVip != null) {
            return ResponseEntity.ok(consultarClienteService.buscarClientes(nombre, esVip));
        }
        return ResponseEntity.ok(consultarClienteService.getAllClientes());
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<HistorialClinicoDTO> getHistorialClinico(@PathVariable UUID id) {
        return ResponseEntity.ok(consultarClienteService.getHistorialClinico(id));
    }

    @PostMapping
    public ResponseEntity<Cliente> registrar(@RequestBody Cliente cliente) {
        Cliente clienteCreado = registrarClienteUseCase.ejecutar(cliente);
        return new ResponseEntity<>(clienteCreado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable UUID id, @RequestBody Cliente cliente) {
        return ResponseEntity.ok(actualizarClienteService.actualizarCliente(id, cliente));
    }
}
