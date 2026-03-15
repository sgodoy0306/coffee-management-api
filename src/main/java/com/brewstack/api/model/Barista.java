package com.brewstack.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "baristas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Barista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer level = 1;

    private Long totalXp = 0L;

    public static int levelForXp(long xp) {
        return (int) Math.floor(Math.sqrt(xp / 100.0)) + 1;
    }
}
