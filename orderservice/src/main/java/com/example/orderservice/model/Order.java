package com.example.orderservice.model;

public class Order {
    private String productId;

    public Order() {
    }

    public Order(String productId) {
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
