package com.fennechome.controller;

public class FennecException extends RuntimeException {
    public FennecException() {
    }

    public FennecException(String message) {
        super(message);
    }

    public FennecException(String message, Throwable cause) {
        super(message, cause);
    }

    public FennecException(Throwable cause) {
        super(cause);
    }

    public FennecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
