package com.nhd.stocksdata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceSearchResponse {
    private List<StockPriceDTO> content;
    private PageMetadata page;

    public boolean isLast() {
        if (page == null || page.getTotalPages() == null || page.getNumber() == null) {
            return true;
        }
        return page.getNumber() >= (page.getTotalPages() - 1);
    }
}
