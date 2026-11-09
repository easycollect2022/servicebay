package com.servicebay.security;

public record VerifiedToken(String uid, String phone, String email, String name) {}
