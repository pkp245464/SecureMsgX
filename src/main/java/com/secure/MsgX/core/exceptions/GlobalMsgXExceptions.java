package com.secure.MsgX.core.exceptions;

public class GlobalMsgXExceptions extends RuntimeException{

    public GlobalMsgXExceptions() {
        super();
    }

    public GlobalMsgXExceptions(String message) {
        super(message);
    }

    public GlobalMsgXExceptions(String message, Throwable throwable) {
        super(message,throwable);
    }

    public GlobalMsgXExceptions(Throwable throwable) {
        super(throwable);
    }
}
