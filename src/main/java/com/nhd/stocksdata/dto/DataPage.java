package com.nhd.stocksdata.dto;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DataPage<T> {
    List<T> content;
    long totalElements;

    public static <T> DataPage<T> from(Page<T> page) {
        return new DataPage<>(page.getContent(), page.getTotalElements());
    }
}
