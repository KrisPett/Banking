package se.sensera.banking.exceptions;

public class UseException extends Exception {
    Activity activity;
    UseExceptionType useExceptionType;
    Object[] params;

    public UseException(Activity activity, UseExceptionType useExceptionType, Object... params) {
        super(activity+" failed because "+ useExceptionType);
        this.activity = activity;
        this.useExceptionType = useExceptionType;
        this.params = params;
    }

    public Activity getActivity() {
        return activity;
    }

    public UseExceptionType getUserExceptionType() {
        return useExceptionType;
    }

    public Object[] getParams() {
        return params;
    }
}
