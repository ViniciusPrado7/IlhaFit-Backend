package com.example.ilhafit.exception;

public class ModeracaoIndisponivelException extends RuntimeException {

    public ModeracaoIndisponivelException(String message) {
        super(message);
    }

    public ModeracaoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
