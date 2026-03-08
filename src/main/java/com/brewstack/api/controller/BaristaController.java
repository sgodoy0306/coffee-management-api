package com.brewstack.api.controller;

import com.brewstack.api.dto.CreateBaristaRequest;
import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.dto.PracticeRequest;
import com.brewstack.api.model.Barista;
import com.brewstack.api.service.BaristaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/baristas")
public class BaristaController {

    private final BaristaService baristaService;

    public BaristaController(BaristaService baristaService) {
        this.baristaService = baristaService;
    }

    @PostMapping
    public ResponseEntity<Barista> createBarista(@Valid @RequestBody CreateBaristaRequest request) {
        Barista barista = baristaService.createBarista(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(barista);
    }

    @PostMapping("/{id}/practice")
    public ResponseEntity<LevelUpDTO> practice(@PathVariable Long id,
                                               @Valid @RequestBody PracticeRequest request) {
        LevelUpDTO result = baristaService.addExperience(id, request.rating());
        return ResponseEntity.ok(result);
    }
}
