package com.example.dsentrictestservice.service;

import com.example.dsentrictestservice.model.LoadTestRequest;
import com.example.dsentrictestservice.model.LoadTestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class LoadTestService {
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public LoadTestService(ObjectMapper objectMapper, @Value("${app.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public LoadTestResponse run(LoadTestRequest request) {
        int total = request.requests();
        int concurrency = Math.max(1, request.concurrency());
        int perWorker = Math.max(1, total / concurrency);
        long start = System.nanoTime();
        int success = 0;
        int failed = 0;
        long totalLatency = 0;

        try (ExecutorService executor = Executors.newFixedThreadPool(concurrency)) {
            Future<long[]>[] futures = new Future[concurrency];
            for (int i = 0; i < concurrency; i++) {
                int calls = i == concurrency - 1 ? total - (perWorker * (concurrency - 1)) : perWorker;
                futures[i] = executor.submit(worker(request, calls));
            }
            for (Future<long[]> future : futures) {
                long[] result = future.get();
                success += (int) result[0];
                failed += (int) result[1];
                totalLatency += result[2];
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Load test interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Load test failed", e);
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        double avgLatency = total == 0 ? 0.0 : (double) totalLatency / total;
        double rps = durationMs == 0 ? total : total / (durationMs / 1000.0);
        return new LoadTestResponse(total, success, failed, durationMs, avgLatency, rps);
    }

    private Callable<long[]> worker(LoadTestRequest request, int calls) {
        return () -> {
            long success = 0;
            long failed = 0;
            long latency = 0;
            for (int i = 0; i < calls; i++) {
                long start = System.nanoTime();
                HttpRequest httpRequest = buildRequest(request);
                try {
                    HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        success++;
                    } else {
                        failed++;
                    }
                } catch (IOException | InterruptedException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    failed++;
                }
                latency += (System.nanoTime() - start) / 1_000_000L;
            }
            return new long[]{success, failed, latency};
        };
    }

    private HttpRequest buildRequest(LoadTestRequest request) {
        String method = request.method() == null ? "GET" : request.method().toUpperCase(Locale.ROOT);
        HttpRequest.BodyPublisher bodyPublisher = request.body() == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(toJson(request.body()));
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + request.endpoint()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .method(method, bodyPublisher)
                .build();
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid load test request body", e);
        }
    }
}
