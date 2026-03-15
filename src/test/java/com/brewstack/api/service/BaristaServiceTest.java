package com.brewstack.api.service;

import com.brewstack.api.dto.BaristaDTO;
import com.brewstack.api.dto.LevelUpDTO;
import com.brewstack.api.exception.BaristaNotFoundException;
import com.brewstack.api.model.Barista;
import com.brewstack.api.repository.BaristaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaristaServiceTest {

    @Mock
    private BaristaRepository baristaRepository;

    @InjectMocks
    private BaristaService baristaService;

    private Barista barista;

    @BeforeEach
    void setUp() {
        barista = new Barista(1L, "Alice", 1, 0L);
    }

    // ── addExperience: level-up formula boundaries ──────────────────────────
    // Formula: level = floor(sqrt(totalXp / 100)) + 1

    @ParameterizedTest(name = "totalXp={0} after adding rating={1} → expectedLevel={2}")
    @DisplayName("addExperience — level formula boundary values")
    @CsvSource({
        // startXp, rating, expectedLevel, expectedTotalXp
        "0,  0,  1,   0",
        "0,  1,  1,  50",
        "49, 1,  1,  99",
        "50, 1,  2, 100",
        "300, 2, 3, 400",
        "300, 1, 2, 350",
        "350, 1, 3, 400",
        "351, 1, 3, 401"
    })
    void addExperience_levelFormulaBoundaries(long startXp, int rating, int expectedLevel, long expectedTotalXp) {
        barista.setTotalXp(startXp);
        barista.setLevel(1);
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        LevelUpDTO result = baristaService.addExperience(1L, rating);

        assertThat(result.totalXp()).isEqualTo(expectedTotalXp);
        assertThat(result.newLevel()).isEqualTo(expectedLevel);
    }

    @Test
    @DisplayName("addExperience — level-up message changes when level increases")
    void addExperience_levelIncreases_returnsLevelUpMessage() {
        barista.setTotalXp(50L);
        barista.setLevel(1);
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        LevelUpDTO result = baristaService.addExperience(1L, 1);

        assertThat(result.newLevel()).isEqualTo(2);
        assertThat(result.message()).contains("level 2");
    }

    @Test
    @DisplayName("addExperience — no level-up message when level stays the same")
    void addExperience_noLevelIncrease_returnsKeepItUpMessage() {
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        LevelUpDTO result = baristaService.addExperience(1L, 1);

        assertThat(result.newLevel()).isEqualTo(1);
        assertThat(result.message()).contains("50 XP");
    }

    @Test
    @DisplayName("addExperience — throws BaristaNotFoundException for unknown barista")
    void addExperience_unknownBarista_throwsNotFoundException() {
        given(baristaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> baristaService.addExperience(99L, 5))
                .isInstanceOf(BaristaNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("addExperience — persists updated barista")
    void addExperience_persistsBarista() {
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        baristaService.addExperience(1L, 2);

        verify(baristaRepository).save(barista);
    }

    // ── deleteBarista ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBarista — fetches entity then delegates to delete(entity)")
    void deleteBarista_happyPath_callsDeleteWithEntity() {
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));

        baristaService.deleteBarista(1L);

        verify(baristaRepository).findById(1L);
        verify(baristaRepository).delete(barista);
    }

    @Test
    @DisplayName("deleteBarista — throws BaristaNotFoundException when barista is absent")
    void deleteBarista_unknownBarista_throwsNotFoundException() {
        given(baristaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> baristaService.deleteBarista(99L))
                .isInstanceOf(BaristaNotFoundException.class)
                .hasMessageContaining("99");

        verify(baristaRepository, never()).delete(any(Barista.class));
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — returns correct DTO for existing barista")
    void findById_existingBarista_returnsCorrectDTO() {
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));

        BaristaDTO result = baristaService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.level()).isEqualTo(1);
        assertThat(result.totalXp()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findById — throws BaristaNotFoundException for unknown barista")
    void findById_unknownBarista_throwsBaristaNotFoundException() {
        given(baristaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> baristaService.findById(99L))
                .isInstanceOf(BaristaNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createBarista ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBarista — new barista starts at level 1 with zero XP")
    void createBarista_newBarista_startsAtLevelOneWithZeroXp() {
        given(baristaRepository.save(any(Barista.class)))
                .willAnswer(inv -> {
                    Barista b = inv.getArgument(0);
                    b.setId(2L);
                    return b;
                });

        BaristaDTO result = baristaService.createBarista("Bob");

        assertThat(result.level()).isEqualTo(1);
        assertThat(result.totalXp()).isEqualTo(0L);
    }

    @Test
    @DisplayName("createBarista — persisted barista DTO carries the requested name")
    void createBarista_newBarista_persistsWithCorrectName() {
        given(baristaRepository.save(any(Barista.class)))
                .willAnswer(inv -> {
                    Barista b = inv.getArgument(0);
                    b.setId(3L);
                    return b;
                });

        BaristaDTO result = baristaService.createBarista("Carol");

        assertThat(result.name()).isEqualTo("Carol");
    }

    // ── updateBarista ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBarista — updates name and returns correct DTO for existing barista")
    void updateBarista_existingBarista_updatesNameAndReturnsDTO() {
        given(baristaRepository.findById(1L)).willReturn(Optional.of(barista));
        given(baristaRepository.save(any(Barista.class))).willAnswer(inv -> inv.getArgument(0));

        BaristaDTO result = baristaService.updateBarista(1L, "Alice Updated");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Alice Updated");
    }

    @Test
    @DisplayName("updateBarista — throws BaristaNotFoundException and never calls save for unknown barista")
    void updateBarista_unknownBarista_throwsBaristaNotFoundException() {
        given(baristaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> baristaService.updateBarista(99L, "Ghost"))
                .isInstanceOf(BaristaNotFoundException.class)
                .hasMessageContaining("99");

        verify(baristaRepository, never()).save(any(Barista.class));
    }
}
