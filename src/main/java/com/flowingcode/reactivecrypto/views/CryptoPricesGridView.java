package com.flowingcode.reactivecrypto.views;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import com.flowingcode.reactivecrypto.application.CryptoPricesSubscriber;
import com.flowingcode.reactivecrypto.application.PriceFluxSubscriptionContext;
import com.flowingcode.reactivecrypto.model.Trade;
import com.flowingcode.reactivecrypto.service.CryptoExchangeService;
import com.flowingcode.reactivecrypto.service.CryptoSymbolService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@PageTitle("Reactive Crypto")
@Route("/grid")
public class CryptoPricesGridView extends VerticalLayout {

    private final PriceFluxSubscriptionContext subscriptionContext;

    private final CryptoExchangeService exchangeService;

    private final ComboBox<String> exchangesComboBox;

    private final Grid<CryptoGridValue> symbolsGrid;

    private final TextField symbolFilterTextField;

    private final Checkbox subscribedCheckBox;

    public CryptoPricesGridView(PriceFluxSubscriptionContext subscriptionContext,
            CryptoExchangeService exchangeService,
            CryptoSymbolService symbolService) {
        this.subscriptionContext = subscriptionContext;
        this.exchangeService = exchangeService;

        setPadding(true);
        setMaxWidth("800px");
        setHeightFull();
        getStyle().set("margin", "0 auto");

        exchangesComboBox = new ComboBox<>("Exchange");
        exchangesComboBox.setWidthFull();

        symbolsGrid = new Grid<>();
        symbolsGrid.setSizeFull();
        symbolsGrid.setSelectionMode(SelectionMode.NONE);

        var symbolColumn = symbolsGrid.addColumn(CryptoGridValue::getDisplaySymbol).setHeader("Symbol").setFlexGrow(3);

        symbolsGrid.addColumn(CryptoGridValue::getPrice).setHeader("Price").setTextAlign(ColumnTextAlign.END).setFlexGrow(1);

        var subscribeColumn = symbolsGrid.addComponentColumn(v -> {
            SubscriptionHolder subscribeButtonWrapper = Optional.ofNullable(v.getSubscription()).orElseGet(() -> new SubscriptionHolder(v));
            Checkbox subscribeButton = new Checkbox();
            subscribeButton.setValue(subscribeButtonWrapper.isSubscribed());
            subscribeButton.addValueChangeListener(e -> {
                if (e.getValue().booleanValue()) {
                    subscriptionContext.subscribe(v.getSymbol(), subscribeButtonWrapper);
                } else {
                    subscriptionContext.unsubscribe(v.getSymbol(), subscribeButtonWrapper);
                }
            });

            return subscribeButton;
        }).setHeader("Subscribe").setTextAlign(ColumnTextAlign.CENTER);

        var headerRow = symbolsGrid.appendHeaderRow();

        symbolFilterTextField = new TextField();
        symbolFilterTextField.setClearButtonVisible(true);
        symbolFilterTextField.setValueChangeMode(ValueChangeMode.EAGER);
        symbolFilterTextField.addValueChangeListener(e -> this.applyFilters());
        headerRow.getCell(symbolColumn).setComponent(symbolFilterTextField);

        subscribedCheckBox = new Checkbox("Subscribed", false);
        subscribedCheckBox.addValueChangeListener(e -> this.applyFilters());
        headerRow.getCell(subscribeColumn).setComponent(subscribedCheckBox);

        exchangesComboBox.addValueChangeListener(e -> {
            cancelSubscriptions();
            clearFilters();

            symbolService.getSymbols(e.getValue(),
                    symbols -> getUI().ifPresent(ui -> ui.access(() -> symbolsGrid.setItems(
                            symbols.stream()
                                    .map(value -> new CryptoGridValue(value.getDisplaySymbol(), value.getSymbol()))
                                    .sorted(Comparator.comparing(CryptoGridValue::getSymbol))
                                    .collect(Collectors.toList())))));
        });

        var formLayout = new FormLayout(exchangesComboBox, symbolsGrid);
        formLayout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("640px", 2));

        var logo = new Image("images/logo.png", "Crypto");
        logo.setWidth("60px");
        logo.setHeight("60px");

        var title = new H1("Realtime Crypto prices with Vaadin and Spring Reactive");

        var titleLayout = new HorizontalLayout(logo, title);
        titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        add(titleLayout, formLayout, symbolsGrid);
        setFlexGrow(1, symbolsGrid);

        exchangesComboBox.focus();
    }

    private void clearFilters() {
        symbolFilterTextField.clear();
        subscribedCheckBox.setValue(false);
    }

    private void cancelSubscriptions() {
        symbolsGrid.getListDataView().getItems()
                .filter(CryptoGridValue::isSubscribed)
                .forEach(item -> subscriptionContext.unsubscribe(item.getSymbol(), item.getSubscription()));
    }

    private void applyFilters() {
        symbolsGrid.getListDataView().removeFilters();
        if (symbolFilterTextField.getValue().length() > 0) {
            symbolsGrid.getListDataView().addFilter(v -> v.getDisplaySymbol().toLowerCase().contains(symbolFilterTextField.getValue().toLowerCase()));
        }
        if (subscribedCheckBox.getValue().booleanValue()) {
            symbolsGrid.getListDataView().addFilter(CryptoGridValue::isSubscribed);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        cancelSubscriptions();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        exchangeService.getExchanges(exchanges -> getUI().ifPresent(ui -> ui.access(() -> {
            exchanges.sort(Comparator.naturalOrder());
            exchangesComboBox.setItems(exchanges);
        })));
    }

    public class SubscriptionHolder implements CryptoPricesSubscriber {

        private final CryptoGridValue value;

        private Disposable subscription;

        public SubscriptionHolder(CryptoGridValue value) {
            this.value = value;
            value.setSubscription(this);
        }

        @Override
        public void subscribe(Flux<Trade> priceFlux, String symbol) {
            subscription = priceFlux.subscribe(trade -> {
                getUI().ifPresent(ui -> {
                    ui.access(() -> {
                        if (value.getSymbol().equals(trade.getSymbol())) {
                            value.setPrice(trade.getPrice());
                            symbolsGrid.getDataProvider().refreshItem(value);
                        }
                    });
                });
            });

            applyFilters();
        }

        @Override
        public void unsubscribe(String symbol) {
            if (isSubscribed()) {
                subscription.dispose();
            }
            subscription = null;

            value.setPrice(null);
            getUI().ifPresent(ui -> ui.access(() -> {
                symbolsGrid.getDataProvider().refreshItem(value);
                applyFilters();
            }));
        }

        public boolean isSubscribed() {
            return subscription != null && !subscription.isDisposed();
        }

    }

}
