package com.flowingcode.reactivecrypto.views;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.flowingcode.reactivecrypto.backend.service.CryptoExchangeService;
import com.flowingcode.reactivecrypto.backend.service.CryptoSymbolService;
import com.flowingcode.reactivecrypto.backend.service.ReactiveStockPricesService;
import com.flowingcode.reactivecrypto.model.CryptoSymbol;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import reactor.core.Disposable;

@PageTitle("Reactive Crypto")
@Route("")
public class CryptoPricesView extends VerticalLayout {

    private final CryptoExchangeService exchangeService;

    private final ComboBox<String> exchangesComboBox;

    private final ComboBox<CryptoSymbol> symbolsComboBox;

    private final Button priceButton;

    private final Button stopButton;

    private transient Disposable apiSuscription;

    private final Map<String, PricePanel> pricePanels = new HashMap<>();

    private final ProgressBar progressBar;

    public CryptoPricesView(ReactiveStockPricesService stockPricesService,
            CryptoExchangeService exchangeService,
            CryptoSymbolService symbolService) {
        this.exchangeService = exchangeService;

        setPadding(true);

        priceButton = new Button("Get price");
        priceButton.setEnabled(false);

        exchangesComboBox = new ComboBox<>("Exchange");

        symbolsComboBox = new ComboBox<>("Symbol");
        symbolsComboBox.setItemLabelGenerator(CryptoSymbol::getDisplaySymbol);

        exchangesComboBox.addValueChangeListener(e -> {
            symbolsComboBox.setEnabled(e.getValue() != null);
            symbolService.getSymbols(e.getValue(), symbols -> getUI().ifPresent(ui -> ui.access(() -> {
                symbols.sort(Comparator.comparing(CryptoSymbol::getDisplaySymbol));
                symbolsComboBox.setItems(symbols);
                symbolsComboBox.focus();
            })));
        });

        symbolsComboBox.addValueChangeListener(e -> {
            priceButton.setEnabled(e.getValue() != null);
            priceButton.focus();
        });

        stopButton = new Button("Stop");

        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);

        add(new H3("Crypto prices with Spring Reactive"), new HorizontalLayout(exchangesComboBox, symbolsComboBox), priceButton, stopButton, progressBar);

        priceButton.addClickListener(e -> {
            progressBar.setVisible(true);
            apiSuscription = stockPricesService.fetchStockPrices(symbolsComboBox.getValue().getSymbol(),
                    trade -> getUI().ifPresent(ui -> ui.access(() -> {
                        progressBar.setVisible(false);
                        PricePanel pricePanel = pricePanels.get(trade.getSymbol());
                        if (pricePanel == null) {
                            pricePanel = new PricePanel();
                            pricePanels.put(trade.getSymbol(), pricePanel);
                            add(pricePanel);
                        }
                        pricePanel.update(trade);
                    })));
            if (apiSuscription != null) {
                priceButton.setVisible(false);
                stopButton.setVisible(true);
                pricePanels.entrySet().forEach(entry -> remove(entry.getValue()));
            }
        });

        stopButton.setVisible(false);
        stopButton.addClickListener(e -> {
            if (apiSuscription != null && !apiSuscription.isDisposed()) {
                apiSuscription.dispose();
            }
            apiSuscription = null;
            stopButton.setVisible(false);
            priceButton.setVisible(true);
            progressBar.setVisible(false);
            pricePanels.entrySet().forEach(entry -> remove(entry.getValue()));
            pricePanels.clear();
        });

        exchangesComboBox.focus();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (apiSuscription != null) {
            apiSuscription.dispose();
        }
        apiSuscription = null;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        exchangeService.getExchanges(exchanges -> getUI().ifPresent(ui -> ui.access(() -> {
            exchanges.sort(Comparator.naturalOrder());
            exchangesComboBox.setItems(exchanges);
        })));
    }
}
