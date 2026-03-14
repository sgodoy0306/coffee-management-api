package com.brewstack.api.repository;

import com.brewstack.api.model.DailyBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyBalanceRepository extends JpaRepository<DailyBalance, LocalDate> {

    Page<DailyBalance> findAllByOrderByDateDesc(Pageable pageable);
}
