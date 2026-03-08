package com.brewstack.api.service;

import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.model.Barista;
import com.brewstack.api.repository.BaristaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BaristaService {

    private final BaristaRepository baristaRepository;

    public BaristaService(BaristaRepository baristaRepository) {
        this.baristaRepository = baristaRepository;
    }

    public Barista createBarista(String name) {
        Barista barista = new Barista();
        barista.setName(name);
        barista.setLevel(1);
        barista.setTotalXp(0L);
        return baristaRepository.save(barista);
    }

    @Transactional
    public LevelUpDTO addExperience(Long baristaId, Integer rating) {
        Barista barista = baristaRepository.findById(baristaId)
                .orElseThrow(() -> new EntityNotFoundException("Barista not found with id: " + baristaId));

        int previousLevel = barista.getLevel();

        long xpGained = (long) rating * 50;
        barista.setTotalXp(barista.getTotalXp() + xpGained);

        int newLevel = (int) Math.floor(Math.sqrt(barista.getTotalXp() / 100.0)) + 1;
        barista.setLevel(newLevel);

        baristaRepository.save(barista);

        String message = newLevel > previousLevel
                ? "Congratulations! You've reached level " + newLevel + "!"
                : "Keep it up! You gained " + xpGained + " XP.";

        return new LevelUpDTO(newLevel, barista.getTotalXp(), message);
    }
}
