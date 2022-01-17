package com.flowingcode.reactivecrypto.views;

import java.util.Comparator;
import java.util.Optional;

import com.flowingcode.reactivecrypto.application.CryptoPricesSubscriber;
import com.flowingcode.reactivecrypto.application.PriceFluxSubscriptionContext;
import com.flowingcode.reactivecrypto.model.CryptoSymbol;
import com.flowingcode.reactivecrypto.model.Trade;
import com.flowingcode.reactivecrypto.service.CryptoExchangeService;
import com.flowingcode.reactivecrypto.service.CryptoSymbolService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@PageTitle("Reactive Crypto")
@Route("/price")
public class CryptoPricesView extends VerticalLayout implements CryptoPricesSubscriber {

    private final PriceFluxSubscriptionContext subscriptionContext;

    private final CryptoExchangeService exchangeService;

    private final ComboBox<String> exchangesComboBox;

    private final ComboBox<CryptoSymbol> symbolsComboBox;

    private final Button subscribeButton;

    private final Button unsubscribeButton;

    private final ProgressBar progressBar;

    private final PricePanel pricePanel;

    private transient Optional<Disposable> priceSubscriptionMaybe = Optional.empty();

    public CryptoPricesView(PriceFluxSubscriptionContext subscriptionContext,
            CryptoExchangeService exchangeService,
            CryptoSymbolService symbolService) {
        this.subscriptionContext = subscriptionContext;
        this.exchangeService = exchangeService;

        setPadding(true);
        setMaxWidth("800px");
        getStyle().set("margin", "0 auto");

        pricePanel = new PricePanel();

        subscribeButton = new Button("Subscribe");
        subscribeButton.setEnabled(false);

        exchangesComboBox = new ComboBox<>("Exchange");
        exchangesComboBox.setWidthFull();

        symbolsComboBox = new ComboBox<>("Crypto");
        symbolsComboBox.setItemLabelGenerator(CryptoSymbol::getDisplaySymbol);
        symbolsComboBox.setWidthFull();
        symbolsComboBox.setEnabled(false);

        exchangesComboBox.addValueChangeListener(e -> {
            symbolService.getSymbols(e.getValue(), symbols -> getUI().ifPresent(ui -> ui.access(() -> {
                symbols.sort(Comparator.comparing(CryptoSymbol::getDisplaySymbol));
                symbolsComboBox.setItems(symbols);
                symbolsComboBox.setEnabled(e.getValue() != null);
                symbolsComboBox.focus();
            })));
        });

        symbolsComboBox.addValueChangeListener(e -> {
            subscribeButton.setEnabled(e.getValue() != null);
            subscribeButton.focus();
        });

        var formLayout = new FormLayout(exchangesComboBox, symbolsComboBox);
        formLayout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("640px", 2));

        unsubscribeButton = new Button("Unsubscribe");

        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);

        var logo = new Image("images/logo.png", "Crypto");
        logo.setWidth("60px");
        logo.setHeight("60px");

        var title = new H1("Realtime Crypto prices with Vaadin and Spring Reactive");

        var titleLayout = new HorizontalLayout(logo, title);
        titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        add(titleLayout, formLayout, subscribeButton, unsubscribeButton, progressBar);

        subscribeButton.addClickListener(e -> {
            var result = subscriptionContext.subscribe(getSymbol(), this);
            if (!result.isOk()) {
                subscribeButton.setVisible(true);
                unsubscribeButton.setVisible(false);
                progressBar.setVisible(false);
            }
        });

        unsubscribeButton.setVisible(false);
        unsubscribeButton.addClickListener(e -> subscriptionContext.unsubscribe(getSymbol(), this));

        exchangesComboBox.focus();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        subscriptionContext.unsubscribe(getSymbol(), this);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        exchangeService.getExchanges(exchanges -> getUI().ifPresent(ui -> ui.access(() -> {
            exchanges.sort(Comparator.naturalOrder());
            exchangesComboBox.setItems(exchanges);
        })));
    }

    @Override
    public void subscribe(Flux<Trade> priceFlux, String symbol) {
        progressBar.setVisible(true);
        subscribeButton.setVisible(false);
        unsubscribeButton.setVisible(true);
        exchangesComboBox.setEnabled(false);
        symbolsComboBox.setEnabled(false);

        var priceSubscription = priceFlux.subscribe(trade -> getUI().ifPresent(ui -> ui.access(() -> {
            if (trade.getSymbol().equals(symbol)) {
                progressBar.setVisible(false);

                pricePanel.setValue(trade);
                if (!pricePanel.isAttached()) {
                    add(pricePanel);
                    setAlignSelf(Alignment.CENTER, pricePanel);
                }
            }
        })));

        // keep for later unsubscription
        priceSubscriptionMaybe = Optional.of(priceSubscription);
    }

    @Override
    public void unsubscribe(String symbol) {
        priceSubscriptionMaybe.ifPresent(Disposable::dispose);
        priceSubscriptionMaybe = Optional.empty();

        remove(pricePanel);
        unsubscribeButton.setVisible(false);
        subscribeButton.setVisible(true);
        progressBar.setVisible(false);
        exchangesComboBox.setEnabled(true);
        symbolsComboBox.setEnabled(true);
    }

    private String getSymbol() {
        return Optional.ofNullable(symbolsComboBox.getValue()).map(CryptoSymbol::getSymbol).orElse("");
    }
}
