package com.brewstack.api.service;

import com.brewstack.api.dto.BaristaDTO;
import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.exception.BaristaNotFoundException;
import com.brewstack.api.model.Barista;
import com.brewstack.api.repository.BaristaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BaristaService {

    private final BaristaRepository baristaRepository;

    public List<BaristaDTO> findAll() {
        return baristaRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public BaristaDTO findById(Long id) {
        Barista barista = baristaRepository.findById(id)
                .orElseThrow(() -> new BaristaNotFoundException(id));
        return toDTO(barista);
    }

    public BaristaDTO createBarista(String name) {
        Barista barista = new Barista();
        barista.setName(name);
        barista.setLevel(1);
        barista.setTotalXp(0L);
        return toDTO(baristaRepository.save(barista));
    }

    @Transactional
    public BaristaDTO updateBarista(Long id, String name) {
        Barista barista = baristaRepository.findById(id)
                .orElseThrow(() -> new BaristaNotFoundException(id));
        barista.setName(name);
        return toDTO(baristaRepository.save(barista));
    }

    private BaristaDTO toDTO(Barista barista) {
        return new BaristaDTO(barista.getId(), barista.getName(), barista.getLevel(), barista.getTotalXp());
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

        int newLevel = Barista.levelForXp(barista.getTotalXp());
        barista.setLevel(newLevel);

        baristaRepository.save(barista);

        String message = newLevel > previousLevel
                ? "Congratulations! You've reached level " + newLevel + "!"
                : "Keep it up! You gained " + xpGained + " XP.";

        return new LevelUpDTO(newLevel, barista.getTotalXp(), message);
    }
}
