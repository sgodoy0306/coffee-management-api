package com.brewstack.api.service;

import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.exception.BaristaNotFoundException;
import com.brewstack.api.model.Barista;
import com.brewstack.api.repository.BaristaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BaristaService {

    private final BaristaRepository baristaRepository;

    public BaristaService(BaristaRepository baristaRepository) {
        this.baristaRepository = baristaRepository;
    }

    public List<Barista> findAll() {
        return baristaRepository.findAll();
    }

    public Barista findById(Long id) {
        return baristaRepository.findById(id)
                .orElseThrow(() -> new BaristaNotFoundException(id));
    }

    public Barista createBarista(String name) {
        Barista barista = new Barista();
        barista.setName(name);
        barista.setLevel(1);
        barista.setTotalXp(0L);
        return baristaRepository.save(barista);
    }

    @Transactional
    public Barista updateBarista(Long id, String name) {
        Barista barista = findById(id);
        barista.setName(name);
        return baristaRepository.save(barista);
    }

    public void deleteBarista(Long id) {
        if (!baristaRepository.existsById(id)) {
            throw new BaristaNotFoundException(id);
        }
        baristaRepository.deleteById(id);
    }

    @Transactional
    public LevelUpDTO addExperience(Long baristaId, Integer rating) {
        Barista barista = baristaRepository.findById(baristaId)
                .orElseThrow(() -> new BaristaNotFoundException(baristaId));

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
