package com.brewstack.api.service;

import com.brewstack.api.dto.BaristaDTO;
import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.exception.BaristaNotFoundException;
import com.brewstack.api.model.Barista;
import com.brewstack.api.repository.BaristaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class BaristaService {

    private final BaristaRepository baristaRepository;

    @Transactional(readOnly = true)
    public List<BaristaDTO> findAll() {
        return baristaRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BaristaDTO findById(Long id) {
        Barista barista = baristaRepository.findById(id)
                .orElseThrow(() -> new BaristaNotFoundException(id));
        return toDTO(barista);
    }

    @Transactional
    public BaristaDTO createBarista(String name) {
        log.info("Creating new barista with name='{}'", name);
        Barista barista = new Barista();
        barista.setName(name);
        barista.setLevel(1);
        barista.setTotalXp(0L);
        BaristaDTO created = toDTO(baristaRepository.save(barista));
        log.info("Barista created successfully with id={}", created.id());
        return created;
    }

    @Transactional
    public BaristaDTO updateBarista(Long id, String name) {
        log.info("Updating barista id={} with new name='{}'", id, name);
        Barista barista = baristaRepository.findById(id)
                .orElseThrow(() -> new BaristaNotFoundException(id));
        barista.setName(name);
        return toDTO(baristaRepository.save(barista));
    }

    private BaristaDTO toDTO(Barista barista) {
        return new BaristaDTO(barista.getId(), barista.getName(), barista.getLevel(), barista.getTotalXp());
    }

    @Transactional
    public void deleteBarista(Long id) {
        log.info("Deleting barista id={}", id);
        Barista barista = baristaRepository.findById(id)
                .orElseThrow(() -> new BaristaNotFoundException(id));
        baristaRepository.delete(barista);
        log.info("Barista id={} deleted successfully", id);
    }

    @Transactional
    public LevelUpDTO addExperience(Long baristaId, Integer rating) {
        log.info("Adding experience to baristaId={} with rating={}", baristaId, rating);
        Barista barista = baristaRepository.findById(baristaId)
                .orElseThrow(() -> new BaristaNotFoundException(baristaId));

        int previousLevel = barista.getLevel();

        long xpGained = (long) rating * 50;
        barista.setTotalXp(barista.getTotalXp() + xpGained);

        int newLevel = Barista.levelForXp(barista.getTotalXp());
        barista.setLevel(newLevel);

        baristaRepository.save(barista);

        if (newLevel > previousLevel) {
            log.info("Barista id={} leveled up from {} to {}", baristaId, previousLevel, newLevel);
        } else {
            log.debug("Barista id={} gained {} XP, remains at level {}", baristaId, xpGained, newLevel);
        }

        String message = newLevel > previousLevel
                ? "Congratulations! You've reached level " + newLevel + "!"
                : "Keep it up! You gained " + xpGained + " XP.";

        return new LevelUpDTO(newLevel, barista.getTotalXp(), message);
    }
}
