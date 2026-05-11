package com.marcog.peluqueria.clientes.application;

import com.marcog.peluqueria.citas.application.GestionarAgenda;
import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.clientes.domain.HistorialClinicoDTO;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultarClientes {

    private final ClienteRepository clienteRepository;
    private final GestionarAgenda gestionarAgenda;

    public List<Cliente> getAllClientes(boolean archivado) {
        return clienteRepository.findAllByArchivado(archivado);
    }

    public List<Cliente> buscarClientes(String nombre, Boolean esVip, boolean archivado) {
        return clienteRepository.findByFiltros(nombre, esVip, archivado);
    }

    public HistorialClinicoDTO getHistorialClinico(UUID clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<Cita> citasList = gestionarAgenda.getCitasByCliente(clienteId);

        return HistorialClinicoDTO.builder()
                .cliente(cliente)
                .citasList(citasList)
                .notasMedicas(cliente.getNotasMedicas())
                .formulasTinte(cliente.getFormulasTinte())
                .build();
    }
}
