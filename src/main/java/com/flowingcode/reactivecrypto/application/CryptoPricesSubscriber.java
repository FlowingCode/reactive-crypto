package com.flowingcode.reactivecrypto.application;

import com.flowingcode.reactivecrypto.model.Trade;

import reactor.core.publisher.Flux;

public interface CryptoPricesSubscriber {

    /**
     * Subscribe crypto price flux.
     * 
     * @param priceFlux flux to sybscribe.
     * @param symbol crypto symbol.
     */
    void subscribe(Flux<Trade> priceFlux, String symbol);

    /**
     * Unsubscribe from price flux.
     * 
     * @param symbol crypto symbol.
     */
    void unsubscribe(String symbol);

}
