package co.com.crediya.model.exceptions;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final BusinessErrorType type;

    public BusinessException(BusinessErrorType type, String message) {
        super(message);
        this.type = type;
    }
}
