package com.nhd.stocksdata.services;

import com.nhd.stocksdata.repository.StockRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public List<String> getAllActiveTickers() {
        return stockRepository.findTickersByActive();
    }
}
