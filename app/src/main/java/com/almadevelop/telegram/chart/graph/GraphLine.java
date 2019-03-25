package com.almadevelop.telegram.chart.graph;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import java.util.List;

public class GraphLine implements GraphObject {
    @NonNull
    private final String label;
    @NonNull
    private final String name;
    @NonNull
    private final List<Long> data;
    private final long topExtremum, lowExtremum;
    @ColorInt
    private final int color;

    public GraphLine(@NonNull String label, @NonNull String name, @NonNull List<Long> data, long topExtremum, long lowExtremum, @ColorInt int color) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (label == null) {
            throw new IllegalArgumentException("Label cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        this.label = label;
        this.name = name;

        this.topExtremum = topExtremum;
        this.lowExtremum = lowExtremum;

        this.data = data;
        this.color = color;
    }

    @NonNull
    @Override
    public List<Long> getData() {
        return data;
    }

    @Override
    public long getTopExtremum() {
        return topExtremum;
    }

    @Override
    public long getLowExtremum() {
        return lowExtremum;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @ColorInt
    public int getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GraphLine line = (GraphLine) o;

        if (topExtremum != line.topExtremum) {
            return false;
        }
        if (lowExtremum != line.lowExtremum) {
            return false;
        }
        if (color != line.color) {
            return false;
        }
        if (!label.equals(line.label)) {
            return false;
        }
        if (!name.equals(line.name)) {
            return false;
        }
        return data.equals(line.data);
    }

    @Override
    public int hashCode() {
        int result = label.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + data.hashCode();
        result = 31 * result + (int) (topExtremum ^ (topExtremum >>> 32));
        result = 31 * result + (int) (lowExtremum ^ (lowExtremum >>> 32));
        result = 31 * result + color;
        return result;
    }

    @Override
    public String toString() {
        return "GraphLine{" +
                "label='" + label + '\'' +
                ", name='" + name + '\'' +
                ", data=" + data +
                ", topExtremum=" + topExtremum +
                ", lowExtremum=" + lowExtremum +
                ", color=" + color +
                '}';
    }
}
