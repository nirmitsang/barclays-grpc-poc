
package com.barclays.monitoring;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@EnableScheduling
public class MetricsWebSocketHandler extends TextWebSocketHandler {
    
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean isRunning = false;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Client connected: {}", session.getId());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Client disconnected: {}", session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> command = objectMapper.readValue(message.getPayload(), Map.class);
        
        if ("start".equals(command.get("command"))) {
            isRunning = true;
            log.info("Starting load test");
        } else if ("stop".equals(command.get("command"))) {
            isRunning = false;
            log.info("Stopping load test");
        }
    }
    
    @Scheduled(fixedDelay = 1000) // Run every second
    public void performLoadTest() {
        if (!isRunning || sessions.isEmpty()) return;
        
        // Perform REST test
        performRestTest();
        
        // Small delay
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        // Perform gRPC test
        performGrpcTest();
    }
    
    private void performRestTest() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request with 50 customers
            List<String> customerIds = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                customerIds.add(String.format("CUST%06d", i));
            }
            
            Map<String, Object> request = new HashMap<>();
            request.put("customerIds", customerIds);
            Map<String, String> filters = new HashMap<>();
            filters.put("industry", "Banking");
            filters.put("riskRating", "Low");
            request.put("metadataFilters", filters);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            // Make request
            ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:8080/api/v1/customers/multi-query",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Send metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("type", "rest");
            metrics.put("responseTime", responseTime);
            metrics.put("recordCount", ((Map)response.getBody()).get("totalCount"));
            
            broadcast(objectMapper.writeValueAsString(metrics));
            
        } catch (Exception e) {
            log.error("REST test error: ", e);
        }
    }
    
    private void performGrpcTest() {
        try {
            // Call gRPC test endpoint
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:8081/test-grpc-performance",
                Map.class
            );
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("type", "grpc");
            metrics.put("responseTime", response.getBody().get("customerQueryTime"));
            metrics.put("recordCount", response.getBody().get("customerCount"));
            
            broadcast(objectMapper.writeValueAsString(metrics));
            
        } catch (Exception e) {
            log.error("gRPC test error: ", e);
        }
    }
    
    private void broadcast(String message) {
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                log.error("Error sending message: ", e);
            }
        });
    }
}
