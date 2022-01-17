package com.flowingcode.reactivecrypto.views;

import java.math.BigDecimal;

import com.flowingcode.reactivecrypto.views.CryptoPricesGridView.SubscriptionHolder;

import lombok.Getter;
import lombok.Setter;

@Getter
public class CryptoGridValue {

    private final String displaySymbol;

    private final String symbol;

    @Setter
    private SubscriptionHolder subscription;

    @Setter
    private BigDecimal price;

    public CryptoGridValue(String displaySymbol, String symbol) {
        this.displaySymbol = displaySymbol;
        this.symbol = symbol;
    }

    public boolean isSubscribed() {
        return subscription != null && subscription.isSubscribed();
    }

}
