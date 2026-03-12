package com.brewstack.api.controller;

import com.brewstack.api.dto.BaristaDTO;
import com.brewstack.api.dto.CreateBaristaRequest;
import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.dto.PracticeRequest;
import com.brewstack.api.dto.UpdateBaristaRequest;
import com.brewstack.api.service.BaristaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/baristas")
public class BaristaController {

    private final BaristaService baristaService;

    @GetMapping
    public ResponseEntity<List<BaristaDTO>> getAllBaristas() {
        return ResponseEntity.ok(baristaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaristaDTO> getBarista(@PathVariable Long id) {
        return ResponseEntity.ok(baristaService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BaristaDTO> createBarista(@Valid @RequestBody CreateBaristaRequest request) {
        BaristaDTO barista = baristaService.createBarista(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(barista);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaristaDTO> updateBarista(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateBaristaRequest request) {
        return ResponseEntity.ok(baristaService.updateBarista(id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBarista(@PathVariable Long id) {
        baristaService.deleteBarista(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/practice")
    public ResponseEntity<LevelUpDTO> practice(@PathVariable Long id,
                                               @Valid @RequestBody PracticeRequest request) {
        LevelUpDTO result = baristaService.addExperience(id, request.rating());
        return ResponseEntity.ok(result);
    }
}
