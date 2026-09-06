package com.neighbourhood.offers.exception;

public class InvalidBillAmountException extends RuntimeException {
    public InvalidBillAmountException(String message) {
        super(message);
    }
}
