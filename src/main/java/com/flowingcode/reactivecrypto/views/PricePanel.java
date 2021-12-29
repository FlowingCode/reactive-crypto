package com.flowingcode.reactivecrypto.views;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import com.flowingcode.reactivecrypto.model.Trade;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

class PricePanel extends VerticalLayout {

    private final Map<Integer, String> PRICE_INDICATORS = Map.of(-1, "price-up", 0, "price-equal", 1, "price-down");

    private final Label symbol;

    private final Label price;

    private Trade previousPrice;

    public PricePanel() {
        setWidth(null);

        getStyle().set("box-shadow", " var(--lumo-box-shadow-xs)");
        symbol = new Label();
        add(symbol);

        price = new Label();
        add(price);
    }

    void update(Trade trade) {
        if (previousPrice == null) {
            previousPrice = trade;
        }

        symbol.setText(trade.getSymbol());

        price.setText("Price: " + Optional.ofNullable(trade.getPrice()).map(BigDecimal::toString).orElse("-"));
        price.setClassName(PRICE_INDICATORS.getOrDefault(previousPrice.compareTo(trade), "price-equal"));

        previousPrice = trade;
    }

}
