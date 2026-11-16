package com.nhd.stocksdata.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "momentum_z_stats",
        schema = "nse",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "mzs_unique_ticker_date",
                    columnNames = {"ticker_list_name", "price_date"})
        })
public class MomentumZStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "ticker_list_name", nullable = false, length = 20)
    private String tickerListName;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "mean_mr12")
    private BigDecimal meanMr12;

    @Column(name = "std_dev_mr12")
    private BigDecimal stdDevMr12;

    @Column(name = "mean_mr6")
    private BigDecimal meanMr6;

    @Column(name = "std_dev_mr6")
    private BigDecimal stdDevMr6;
}
