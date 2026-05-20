package com.openway.periplus.model;

public record ProductSummary(
        String productId,
        String isbn,
        String title,
        long unitPrice
) {
}
