package com.neighbourhood.offers.exception;

public class UnauthorizedShopAccessException extends RuntimeException {
    public UnauthorizedShopAccessException(String message) {
        super(message);
    }
}
