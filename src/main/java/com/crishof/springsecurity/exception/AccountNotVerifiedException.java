package com.crishof.springsecurity.exception;

public class AccountNotVerifiedException extends RuntimeException {

    public AccountNotVerifiedException(String message) {
        super(message);
    }
}
