package com.brewstack.api.controller;

import com.brewstack.api.dto.CreatePastryRequest;
import com.brewstack.api.dto.PastryDTO;
import com.brewstack.api.dto.UpdatePastryRequest;
import com.brewstack.api.service.PastryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pastries")
@RequiredArgsConstructor
public class PastryController {

    private final PastryService pastryService;

    @PostMapping
    public ResponseEntity<PastryDTO> create(@Valid @RequestBody CreatePastryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pastryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PastryDTO>> getAll() {
        return ResponseEntity.ok(pastryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PastryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pastryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PastryDTO> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdatePastryRequest request) {
        return ResponseEntity.ok(pastryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pastryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
