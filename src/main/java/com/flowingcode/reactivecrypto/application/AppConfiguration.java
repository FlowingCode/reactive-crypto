package com.flowingcode.reactivecrypto.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flowingcode.reactivecrypto.model.SymbolRequest;
import com.flowingcode.reactivecrypto.model.Trade;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import reactor.util.concurrent.Queues;

@Configuration
public class AppConfiguration {

    @Bean
    WebSocketClient webSocketClient() {
        return new ReactorNettyWebSocketClient();
    }

    @Bean
    WebClient finnhubWebClient(@Value("${api.finnhub.base.endpoint}") String baseUrl,
            @Value("${api.finnhub.token}") String token,
            WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(baseUrl)
                .defaultHeader("X-Finnhub-Token", token)
                .build();
    }

    /**
     * Sink for publishing crypto prices requests.
     * 
     * @return
     */
    @Bean
    Many<SymbolRequest> requestSink() {
        return Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    /**
     * Flux of crypto prices requests.
     * 
     * @return
     */
    @Bean
    Flux<SymbolRequest> requestFlux() {
        return requestSink().asFlux();
    }

    /**
     * Sink for publishing crypto prices.
     * 
     * @return
     */
    @Bean
    Many<Trade> priceSink() {
        // set autoCancel=false to avoid Sink completion when last subscriber cancels
        return Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    /**
     * Flux of crypto prices.
     * 
     * @return
     */
    @Bean
    Flux<Trade> priceFlux() {
        return priceSink().asFlux();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .registerModule(new JavaTimeModule());
    }

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

    @Bean
    Jackson2JsonDecoder jackson2JsonDecoder() {
        return new Jackson2JsonDecoder(objectMapper());
    }

    @Bean
    Jackson2JsonEncoder jackson2JsonEncoder() {
        return new Jackson2JsonEncoder(objectMapper());
    }

}
