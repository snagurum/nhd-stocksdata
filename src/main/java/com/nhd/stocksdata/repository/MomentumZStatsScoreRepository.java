package com.nhd.stocksdata.repository;

import com.nhd.stocksdata.models.MomentumZStatsScore;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MomentumZStatsScoreRepository extends JpaRepository<MomentumZStatsScore, Long> {

    Page<MomentumZStatsScore> findByPriceDate(LocalDate date, Pageable pageable);
}
