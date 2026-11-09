package com.servicebay.service;

import java.util.Map;

public record PushEvent(Long userId, String title, String body, Map<String, String> data) {}
