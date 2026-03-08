package com.brewstack.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "latte_art_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatteArtSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "barista_id")
    private Barista barista;

    private String pattern;

    private Integer score;

    private LocalDateTime practicedAt;
}
