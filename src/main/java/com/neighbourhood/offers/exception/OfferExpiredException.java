package com.neighbourhood.offers.exception;

public class OfferExpiredException extends RuntimeException {
    public OfferExpiredException(String message) {
        super(message);
    }
}
