package com.flowingcode.reactivecrypto.backend.service;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowingcode.reactivecrypto.model.SymbolRequest;
import com.flowingcode.reactivecrypto.model.Trade;
import com.flowingcode.reactivecrypto.model.TradeResponse;
import com.flowingcode.reactivecrypto.model.TradeResponse.Types;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Service
public class ReactiveStockPricesService {

    private static Logger log = LoggerFactory.getLogger(ReactiveStockPricesService.class);

    @Value("${api.finnhub.endpoint}")
    private String webserviceEnpoint;

    private final ObjectMapper objectMapper;

    private final WebSocketClient webSocketClient;

    ReactiveStockPricesService(ObjectMapper objectMapper,
            WebSocketClient webSocketClient) {
        this.objectMapper = objectMapper;
        this.webSocketClient = webSocketClient;
    }

    public Disposable fetchStockPrices(String symbol, Consumer<Trade> consumer) {
        HttpHeaders headers = new HttpHeaders();
        final Mono<String> mono = Mono.create(sink -> {
            try {
                String payload = objectMapper.writeValueAsString(new SymbolRequest("subscribe", symbol));
                sink.success(payload);
            } catch (JsonProcessingException e) {
                sink.error(e);
            }
        });

        return webSocketClient.execute(
                URI.create(webserviceEnpoint), headers,
                session -> session
                        .send(mono.map(session::textMessage))
                        .thenMany(session.receive()
                                .filter(message -> message.getType() == WebSocketMessage.Type.TEXT) // Filter out non-text messages
                                .map(WebSocketMessage::getPayloadAsText)
                                .log()
                                .map(message -> {
                                    try {
                                        return objectMapper.readValue(message, TradeResponse.class);
                                    } catch (JsonProcessingException e) {
                                        log.error("Error", e);
                                        return new TradeResponse();
                                    }
                                })
                                .filter(tradeResponse -> Types.TRADE.getType().equals(tradeResponse.getType()))
                                .map(TradeResponse::getTrades)
                                .filter(Objects::nonNull)
                                .map(trades -> trades.stream().findFirst().orElse(new Trade()))
                                .doOnNext(consumer::accept))
                        .then())
                .subscribe();
    }

}