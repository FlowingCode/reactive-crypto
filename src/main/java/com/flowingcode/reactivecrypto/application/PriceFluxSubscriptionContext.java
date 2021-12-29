package com.flowingcode.reactivecrypto.application;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.flowingcode.reactivecrypto.model.SymbolRequest;
import com.flowingcode.reactivecrypto.model.Trade;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.publisher.Sinks.Many;

/**
 * Stateful singleton bean that handles subscriptions to price flux. <br/>
 * Supports multiple subscribers to a single crypto symbol.
 * 
 * @author flang
 *
 */
@Component
public class PriceFluxSubscriptionContext {

    @Autowired
    private Many<SymbolRequest> requestSink;

    @Autowired
    private Flux<Trade> priceFlux;

    private final Map<String, List<CryptoPricesSubscriber>> subscribers = new ConcurrentHashMap<>();

    public SubscriptionResult subscribe(String symbol, CryptoPricesSubscriber subscriber) {
        synchronized (subscribers) {
            var symbolSubscribers = subscribers.computeIfAbsent(symbol, key -> new CopyOnWriteArrayList<>());
            symbolSubscribers.add(subscriber);

            var emitResult = EmitResult.OK;

            if (symbolSubscribers.size() == 1) {
                // emit request only if this is the first subscriber
                emitResult = requestSink.tryEmitNext(SymbolRequest.subscribe(symbol));
            }

            if (emitResult.isSuccess()) {
                subscriber.subscribe(priceFlux, symbol);
                return SubscriptionResult.ok();
            }

            return SubscriptionResult.error();
        }
    }

    public void unsubscribe(String symbol, CryptoPricesSubscriber subscriber) {
        synchronized (subscribers) {
            var symbolSubscribers = subscribers.get(symbol);

            if (symbolSubscribers != null) {
                symbolSubscribers.remove(subscriber);
                if (symbolSubscribers.isEmpty()) {
                    // emit an unsubscribe request if symbol has no more subscribers
                    requestSink.tryEmitNext(SymbolRequest.unsubscribe(symbol));
                }
            }

            subscriber.unsubscribe();
        }
    }

    public static class SubscriptionResult {

        private final boolean result;

        SubscriptionResult(boolean result) {
            this.result = result;
        }

        static SubscriptionResult ok() {
            return new SubscriptionResult(true);
        }

        static SubscriptionResult error() {
            return new SubscriptionResult(false);
        }

        public boolean isOk() {
            return result;
        }
    }

}
