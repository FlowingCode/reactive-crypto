package com.flowingcode.reactivecrypto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SymbolRequest {

    private String type;

    private String symbol;

}