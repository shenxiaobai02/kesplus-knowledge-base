package com.kes.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SseEmitterHelper {

    private static final long DEFAULT_TIMEOUT = 30000L;
    
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public static SseEmitter create(String clientId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        
        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            log.debug("SSE connection completed for client: {}", clientId);
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            log.debug("SSE connection timeout for client: {}", clientId);
        });
        
        emitter.onError(e -> {
            emitters.remove(clientId);
            log.debug("SSE connection error for client: {}", clientId, e);
        });
        
        emitters.put(clientId, emitter);
        return emitter;
    }

    public static void send(String clientId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.error("Failed to send SSE event to client: {}", clientId, e);
                emitters.remove(clientId);
            }
        }
    }

    public static void complete(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("Failed to complete SSE connection for client: {}", clientId, e);
            }
        }
    }

    public static void sendError(String clientId, String errorMessage) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("error").data(errorMessage));
                emitter.completeWithError(new RuntimeException(errorMessage));
            } catch (IOException e) {
                log.error("Failed to send error to client: {}", clientId, e);
            }
        }
    }

    public static int getActiveConnections() {
        return emitters.size();
    }
}