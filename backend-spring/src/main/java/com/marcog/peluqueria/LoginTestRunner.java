package com.marcog.peluqueria;

import com.marcog.peluqueria.security.application.dto.AuthRequest;
import com.marcog.peluqueria.security.application.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LoginTestRunner implements CommandLineRunner {

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("===== STARTING LOGIN TEST =====");
            AuthRequest request = new AuthRequest("admin", "1234", true);
            var response = authenticationService.authenticate(request);
            System.out.println("TEST SUCCESS: " + response.getToken());
            System.out.println("==============================");
        } catch (Exception e) {
            System.err.println("===== LOGIN TEST FAILED =====");
            e.printStackTrace();
            System.err.println("==============================");
        }
    }
}
