package com.sportscenter.invoice;

/** Deliberately carries only a fixed reason, never provider bodies, URLs or credentials. */
public class VnPayQueryException extends RuntimeException {
    public VnPayQueryException(String reason) { super(reason); }
}
