package com.flowingcode.reactivecrypto.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(Include.NON_NULL)
public class SymbolRequest {

    private String type;

    private String symbol;

    public static SymbolRequest subscribe(String symbol) {
        return new SymbolRequest("subscribe", symbol);
    }

    public static SymbolRequest unsubscribe(String symbol) {
        return new SymbolRequest("unsubscribe", symbol);
    }

}