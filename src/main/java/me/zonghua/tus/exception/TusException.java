package me.zonghua.tus.exception;

public class TusException extends RuntimeException {
    public TusException(String message) {
        super(message);
    }

    public TusException(String message, Throwable cause) {
        super(message, cause);
    }
}
