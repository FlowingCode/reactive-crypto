package com.flowingcode.reactivecrypto.backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flowingcode.reactivecrypto.model.CryptoSymbol;

@Service
public class CryptoSymbolService {

    @Value("${api.finnhub.token}")
    private String apiToken;

    @Value("${api.finnhub.crypto.symbols.endpoint}")
    private String apiEnpoint;

    private final WebClient webClient;

    public CryptoSymbolService(WebClient webClient) {
        this.webClient = webClient;
    }

    public void getSymbols(String exchange, Consumer<List<CryptoSymbol>> consumer) {
        webClient.get()
                .uri(builder -> builder.path(apiEnpoint).queryParam("exchange", exchange).build())
                .retrieve().bodyToMono(CryptoSymbol[].class)
                .subscribe(v -> consumer.accept(Arrays.asList(v)));
    }

}
