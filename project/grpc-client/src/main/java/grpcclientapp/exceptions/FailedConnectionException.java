package grpcclientapp.exceptions;

public class FailedConnectionException extends Exception {
    public FailedConnectionException(String message) {
        super(message);
    }
}
