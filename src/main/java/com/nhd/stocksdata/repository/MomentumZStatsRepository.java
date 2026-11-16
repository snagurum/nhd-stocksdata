package com.nhd.stocksdata.repository;

import com.nhd.stocksdata.models.MomentumZStats;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface MomentumZStatsRepository extends JpaRepository<MomentumZStats, Long> {

    Page<MomentumZStats> findByPriceDate(LocalDate date, Pageable pageable);

    MomentumZStats findByPriceDateAndTickerListName(LocalDate date, String tickerListName);
}
