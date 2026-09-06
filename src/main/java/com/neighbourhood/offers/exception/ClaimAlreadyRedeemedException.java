package com.neighbourhood.offers.exception;

public class ClaimAlreadyRedeemedException extends RuntimeException {
    public ClaimAlreadyRedeemedException(String message) {
        super(message);
    }
}
