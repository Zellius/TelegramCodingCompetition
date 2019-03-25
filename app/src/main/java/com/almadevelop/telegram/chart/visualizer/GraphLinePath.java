package com.almadevelop.telegram.chart.visualizer;

import android.graphics.Path;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

class GraphLinePath {
    @NonNull
    private final Path path;

    @NonNull
    private final String label;

    private final long topExtremum;
    private final long lowExtremum;

    @ColorInt
    private final int color;

    private int colorAlpha = 255;

    GraphLinePath(@NonNull Path path, @NonNull String label, long topExtremum, long lowExtremum, @ColorInt int color) {
        this.path = path;
        this.label = label;
        this.topExtremum = topExtremum;
        this.lowExtremum = lowExtremum;
        this.color = color;
    }

    boolean isVisible() {
        return colorAlpha == 255;
    }

    boolean isInvisible() {
        return colorAlpha == 0;
    }

    @NonNull
    Path getPath() {
        return path;
    }

    @NonNull
    String getLabel() {
        return label;
    }

    long getTopExtremum() {
        return topExtremum;
    }

    long getLowExtremum() {
        return lowExtremum;
    }

    @ColorInt
    int getColor() {
        return color;
    }

    int getColorAlpha() {
        return colorAlpha;
    }

    void setColorAlpha(int colorAlpha) {
        this.colorAlpha = colorAlpha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GraphLinePath that = (GraphLinePath) o;

        if (topExtremum != that.topExtremum) {
            return false;
        }
        if (lowExtremum != that.lowExtremum) {
            return false;
        }
        if (color != that.color) {
            return false;
        }
        if (colorAlpha != that.colorAlpha) {
            return false;
        }
        if (!path.equals(that.path)) {
            return false;
        }
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + label.hashCode();
        result = 31 * result + (int) (topExtremum ^ (topExtremum >>> 32));
        result = 31 * result + (int) (lowExtremum ^ (lowExtremum >>> 32));
        result = 31 * result + color;
        result = 31 * result + colorAlpha;
        return result;
    }

    @Override
    public String toString() {
        return "GraphLinePath{" +
                "path=" + path +
                ", label='" + label + '\'' +
                ", topExtremum=" + topExtremum +
                ", lowExtremum=" + lowExtremum +
                ", color=" + color +
                ", colorAlpha=" + colorAlpha +
                '}';
    }
}
