package com.fptaptech.techshop.order.common;

public class PreconditionFailedException extends RuntimeException {
    private final String expectedETag;
    private final String actualETag;

    public PreconditionFailedException(String expectedETag, String actualETag, String message) {
        super(message);
        this.expectedETag = expectedETag;
        this.actualETag = actualETag;
    }

    public String getExpectedETag() {
        return expectedETag;
    }

    public String getActualETag() {
        return actualETag;
    }
}
