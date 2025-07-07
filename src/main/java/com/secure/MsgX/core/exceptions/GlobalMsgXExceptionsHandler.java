package com.secure.MsgX.core.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Date;

@ControllerAdvice
public class GlobalMsgXExceptionsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalMsgXExceptionsHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorInfo> handleRuntimeExceptionForOthers(HttpServletRequest req, RuntimeException ex) {
        LOGGER.error("GlobalMsgXExceptions occurred: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED).body(new ErrorInfo(req.getRequestURL().toString(), ex.getLocalizedMessage(), String.valueOf(ex.getMessage()), new Date()));
    }

    @ExceptionHandler(GlobalMsgXExceptions.class)
    public ResponseEntity<ErrorInfo> handlerGlobalDurinUserServiceException(GlobalMsgXExceptions globalMsgXExceptions) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorInfo(
                null,
                "GlobalMsgXExceptions",
                globalMsgXExceptions.getMessage(),
                new Date()
        ));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorInfo> handleNullPointerException(HttpServletRequest httpServletRequest, NullPointerException nullPointerException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorInfo(
                httpServletRequest.getRequestURL().toString(),
                "NullPointerException",
                nullPointerException.getMessage(),
                new Date()
        ));
    }
}
