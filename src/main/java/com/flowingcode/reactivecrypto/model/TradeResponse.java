package com.flowingcode.reactivecrypto.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeResponse {

    public enum Types {

        TRADE("trade"),
        PING("ping");

        @Getter
        private final String type;

        Types(String type) {
            this.type = type;
        }

    }

    @JsonProperty("type")
    private String type;

    @JsonProperty("data")
    private List<Trade> trades;

}