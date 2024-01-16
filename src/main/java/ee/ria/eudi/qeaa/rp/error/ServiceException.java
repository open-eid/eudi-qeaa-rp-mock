package ee.ria.eudi.qeaa.rp.error;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public ServiceException(String message) {
        this(ErrorCode.INVALID_REQUEST, message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INVALID_REQUEST;
    }

    public ServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
