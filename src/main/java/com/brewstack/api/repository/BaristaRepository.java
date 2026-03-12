package com.brewstack.api.repository;

import com.brewstack.api.model.Barista;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaristaRepository extends JpaRepository<Barista, Long> {
}
