package com.brewstack.api.repository;

import com.brewstack.api.model.Pastry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PastryRepository extends JpaRepository<Pastry, Long> {

    boolean existsByName(String name);

    List<Pastry> findAllByAvailableTrue();
}
