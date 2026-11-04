package com.sportscenter.auth;

public interface GoogleTokenVerifier {
    GoogleIdentity verify(String credential);
}
