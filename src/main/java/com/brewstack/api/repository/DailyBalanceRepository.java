package com.brewstack.api.repository;

import com.brewstack.api.model.DailyBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyBalanceRepository extends JpaRepository<DailyBalance, LocalDate> {

    List<DailyBalance> findAllByOrderByDateDesc();
}
