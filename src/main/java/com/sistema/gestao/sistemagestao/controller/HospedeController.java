package com.sistema.gestao.sistemagestao.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class HospedeController {

    @GetMapping
    public String getMethodName() {
        return "Teste";
    }
    
}
