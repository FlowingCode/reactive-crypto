package com.flowingcode.reactivecrypto.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({ "c" })
public class Trade implements Comparable<Trade> {

    @JsonProperty("p")
    private BigDecimal price;

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("t")
    private Instant timeStamp;

    @JsonProperty("v")
    private BigDecimal volume;

    @Override
    public int compareTo(Trade o) {
        if (price == null)
            return -1;
        if (o.price == null)
            return 1;
        return price.compareTo(o.price);
    }

}