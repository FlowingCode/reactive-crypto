package com.flowingcode.reactivecrypto.service;

import java.net.URI;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowingcode.reactivecrypto.model.SymbolRequest;
import com.flowingcode.reactivecrypto.model.Trade;
import com.flowingcode.reactivecrypto.model.TradeResponse;
import com.flowingcode.reactivecrypto.model.TradeResponse.Types;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks.Many;

@Service
@RequiredArgsConstructor
public class CryptoPricesService implements WebSocketHandler {

    @Value("${api.finnhub.endpoint}")
    private String webserviceEnpoint;

    private final ObjectMapper objectMapper;

    private final WebSocketClient webSocketClient;

    private final Many<Trade> priceSink;

    private final Flux<SymbolRequest> requestFlux;

    // WS subscription state variable for later cleanup
    private Disposable webServiceSubscription;

    @PostConstruct
    void init() {
        // open websocket session
        webServiceSubscription = webSocketClient.execute(URI.create(webserviceEnpoint), this).subscribe();
    }

    @PreDestroy
    void destroy() {
        if (webServiceSubscription != null) {
            webServiceSubscription.dispose();
        }
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .log()
                .map(this::map)
                .filter(tradeResponse -> Types.TRADE.getType().equals(tradeResponse.getType()))
                .map(TradeResponse::getTrades)
                .filter(Objects::nonNull)
                .flatMap(trades -> Flux.fromStream(trades.stream()))
                .subscribe(priceSink::tryEmitNext);

        return session.send(requestFlux.map(this::map).map(session::textMessage))
                .then();
    }

    @SneakyThrows
    private String map(SymbolRequest request) {
        return objectMapper.writeValueAsString(request);
    }

    @SneakyThrows
    private TradeResponse map(String json) {
        return objectMapper.readValue(json, TradeResponse.class);
    }

}