package com.brewstack.api.service;

import com.brewstack.api.dto.CreatePastryRequest;
import com.brewstack.api.dto.PastryDTO;
import com.brewstack.api.dto.UpdatePastryRequest;
import com.brewstack.api.exception.PastryNotFoundException;
import com.brewstack.api.model.Pastry;
import com.brewstack.api.repository.PastryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PastryService {

    private final PastryRepository pastryRepository;

    @Transactional(readOnly = true)
    public List<PastryDTO> findAll() {
        return pastryRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public PastryDTO findById(Long id) {
        return toDTO(findEntityById(id));
    }

    @Transactional
    public PastryDTO create(CreatePastryRequest request) {
        log.info("Creating pastry name='{}'", request.name());
        Pastry pastry = new Pastry(null, request.name(), request.description(),
                request.price(), request.available());
        PastryDTO created = toDTO(pastryRepository.save(pastry));
        log.info("Pastry created id={}", created.id());
        return created;
    }

    @Transactional
    public PastryDTO update(Long id, UpdatePastryRequest request) {
        log.info("Updating pastry id={}", id);
        Pastry pastry = findEntityById(id);
        pastry.setName(request.name());
        pastry.setDescription(request.description());
        pastry.setPrice(request.price());
        pastry.setAvailable(request.available());
        return toDTO(pastryRepository.save(pastry));
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting pastry id={}", id);
        Pastry pastry = findEntityById(id);
        pastryRepository.delete(pastry);
        log.info("Pastry id={} deleted", id);
    }

    private Pastry findEntityById(Long id) {
        return pastryRepository.findById(id)
                .orElseThrow(() -> new PastryNotFoundException(id));
    }

    private PastryDTO toDTO(Pastry pastry) {
        return new PastryDTO(pastry.getId(), pastry.getName(), pastry.getDescription(),
                pastry.getPrice(), pastry.isAvailable());
    }
}
