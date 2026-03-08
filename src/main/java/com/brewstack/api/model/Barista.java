package com.brewstack.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "baristas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "sessions")
@EqualsAndHashCode(exclude = "sessions")
public class Barista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer level = 1;

    private Long totalXp = 0L;

    @OneToMany(mappedBy = "barista", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LatteArtSession> sessions;
}
