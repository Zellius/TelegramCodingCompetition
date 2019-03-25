package com.almadevelop.telegram.chart.parser;

public class IllegalChartJsonFormat extends RuntimeException {
    public IllegalChartJsonFormat(String message) {
        super(message);
    }
}
