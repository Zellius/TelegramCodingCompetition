package com.almadevelop.telegram.chart.graph;

import android.support.annotation.NonNull;

import java.util.List;

public class GraphAxis implements GraphObject {
    @NonNull
    private final List<Long> data;

    private final long topPeak, lowestPeak;

    public GraphAxis(@NonNull List<Long> data, long topPeak, long lowestPeak) {
        if (data == null) {
            throw new IllegalArgumentException("Axis data cannot be null");
        }

        this.data = data;
        this.topPeak = topPeak;
        this.lowestPeak = lowestPeak;
    }

    @NonNull
    @Override
    public List<Long> getData() {
        return data;
    }

    @Override
    public long getTopExtremum() {
        return topPeak;
    }

    @Override
    public long getLowExtremum() {
        return lowestPeak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GraphAxis graphAxis = (GraphAxis) o;

        if (topPeak != graphAxis.topPeak) {
            return false;
        }
        if (lowestPeak != graphAxis.lowestPeak) {
            return false;
        }
        return data.equals(graphAxis.data);
    }

    @Override
    public int hashCode() {
        int result = data.hashCode();
        result = 31 * result + (int) (topPeak ^ (topPeak >>> 32));
        result = 31 * result + (int) (lowestPeak ^ (lowestPeak >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "GraphAxis{" +
                "data=" + data +
                ", topPeak=" + topPeak +
                ", lowestPeak=" + lowestPeak +
                '}';
    }
}
