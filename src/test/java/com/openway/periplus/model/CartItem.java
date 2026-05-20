package com.openway.periplus.model;

public record CartItem(
        String productId,
        String isbn,
        String title,
        int quantity,
        long unitPrice
) {
    public long lineSubtotal() {
        return unitPrice * quantity;
    }
}
