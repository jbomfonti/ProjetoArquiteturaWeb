package com.sistema.gestao.sistemagestao.controller;

import org.springframework.web.bind.annotation.RestController;

import com.sistema.gestao.sistemagestao.model.Hospede;
import com.sistema.gestao.sistemagestao.service.HospedeService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/hospedes")
public class HospedeController {

    @Autowired
    private HospedeService service;

    @PostMapping
    public ResponseEntity<Hospede> criar(@RequestBody Hospede hospede) {
        return ResponseEntity.ok(service.criar(hospede));
    }

    @GetMapping
    public ResponseEntity<List<Hospede>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hospede> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Hospede> atualizar(
            @PathVariable Long id,
            @RequestBody Hospede hospede) {

        return ResponseEntity.ok(service.atualizar(id, hospede));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
