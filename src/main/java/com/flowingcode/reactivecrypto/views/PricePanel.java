package com.flowingcode.reactivecrypto.views;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import com.flowingcode.reactivecrypto.model.Trade;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

class PricePanel extends CustomField<Trade> {

    private static final Map<Integer, String> PRICE_INDICATORS = Map.of(-1, "price-up", 0, "price-equal", 1, "price-down");

    private final Label price;

    private Trade previousPrice;

    public PricePanel() {
        VerticalLayout root = new VerticalLayout();
        root.setWidth(null);
        root.setClassName("price-panel");
        root.setAlignItems(Alignment.CENTER);

        price = new Label();
        root.add(price);

        add(root);
    }

    @Override
    protected Trade generateModelValue() {
        return previousPrice;
    }

    @Override
    protected void setPresentationValue(Trade value) {
        if (previousPrice == null) {
            previousPrice = value;
        }

        price.setText("Price: " + Optional.ofNullable(value.getPrice()).map(BigDecimal::toString).orElse("-"));
        price.setClassName(PRICE_INDICATORS.getOrDefault(previousPrice.compareTo(value), "price-equal"));

        previousPrice = value;
    }

}
