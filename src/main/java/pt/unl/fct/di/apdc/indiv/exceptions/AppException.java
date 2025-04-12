package pt.unl.fct.di.apdc.indiv.exceptions;

public class AppException extends Exception {
    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}