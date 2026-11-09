package com.servicebay.service;

import java.util.List;
import java.util.Map;

public interface PushSender {
    /** Sends to every token; returns tokens that are no longer valid and should be deleted. */
    List<String> send(List<String> tokens, String title, String body, Map<String, String> data);
}
