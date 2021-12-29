package com.flowingcode.reactivecrypto.service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CryptoExchangeService {

    @Value("${api.finnhub.crypto.exchange.endpoint}")
    private String apiEnpoint;

    private final WebClient webClient;

    public CryptoExchangeService(WebClient webClient) {
        this.webClient = webClient;
    }

    public void getExchanges(Consumer<List<String>> consumer) {
        webClient.get()
                .uri(apiEnpoint)
                .retrieve()
                .bodyToMono(String[].class)
                .subscribe(v -> consumer.accept(Arrays.asList(v)));
    }

}
