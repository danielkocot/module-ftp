package com.reedelk.ftp.internal.exception;

import com.reedelk.runtime.api.exception.PlatformException;

public class FTPDeleteException extends PlatformException {

    public FTPDeleteException(String message) {
        super(message);
    }

    public FTPDeleteException(String message, Throwable exception) {
        super(message, exception);
    }
}
