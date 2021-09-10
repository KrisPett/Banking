package se.sensera.banking.exceptions;

import java.util.function.Consumer;
import java.util.function.Function;

public interface HandleException {
    static <E extends Exception, R> R safe(Safe<E,R> safe, Function<Exception,String> errMsg) {
        try {
            return safe.execute();
        } catch (Exception e) {
            throw new RuntimeException(errMsg.apply((E) e), e);
        }
    }

    interface Safe<E extends Exception, R> {
        R execute() throws E;
    }
}
