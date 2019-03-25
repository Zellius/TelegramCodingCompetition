package com.almadevelop.telegram.chart.visualizer;

import android.graphics.RectF;
import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * Describes selected graph's window
 */
class SelectedGraphWindow {
    @NonNull
    private final GraphLinePath[] lines;
    @NonNull
    private final RectF bounds;
    @NonNull
    private final long[] xValues;
    @NonNull
    private final long[][] yValues;
    private final long yTopExtremum, yLowExtremum;

    /**
     * @param lines       calculated line segments
     * @param bounds  bounds of the calculated segments
     * @param xValues     x values for selected window
     * @param yValues     y values for selected window
     * @param yTopExtremum Y axis top value of the segment
     * @param yLowExtremum Y axis low value of the segment
     */
    SelectedGraphWindow(@NonNull GraphLinePath[] lines,
                        @NonNull RectF bounds,
                        @NonNull long[] xValues,
                        @NonNull long[][] yValues,
                        long yTopExtremum,
                        long yLowExtremum) {
        this.lines = lines;
        this.bounds = bounds;
        this.xValues = xValues;
        this.yValues = yValues;
        this.yTopExtremum = yTopExtremum;
        this.yLowExtremum = yLowExtremum;
    }

    @NonNull
    GraphLinePath[] getLines() {
        return lines;
    }

    @NonNull
    RectF getBounds() {
        return bounds;
    }

    @NonNull
    long[] getxValues() {
        return xValues;
    }

    long getxTopExtremum() {
        return xValues[xValues.length - 1];
    }

    long getxLowExtremum() {
        return xValues[0];
    }

    @NonNull
    long[][] getyValues() {
        return yValues;
    }

    long getyTopExtremum() {
        return yTopExtremum;
    }

    long getyLowExtremum() {
        return yLowExtremum;
    }

    boolean hasSameExtremums(SelectedGraphWindow otherWindow) {
        return otherWindow.yTopExtremum == yTopExtremum && otherWindow.yLowExtremum == yLowExtremum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SelectedGraphWindow that = (SelectedGraphWindow) o;

        if (yTopExtremum != that.yTopExtremum) {
            return false;
        }
        if (yLowExtremum != that.yLowExtremum) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(lines, that.lines)) {
            return false;
        }
        if (!bounds.equals(that.bounds)) {
            return false;
        }
        if (!Arrays.equals(xValues, that.xValues)) {
            return false;
        }
        return Arrays.deepEquals(yValues, that.yValues);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(lines);
        result = 31 * result + bounds.hashCode();
        result = 31 * result + Arrays.hashCode(xValues);
        result = 31 * result + Arrays.deepHashCode(yValues);
        result = 31 * result + (int) (yTopExtremum ^ (yTopExtremum >>> 32));
        result = 31 * result + (int) (yLowExtremum ^ (yLowExtremum >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "SelectedGraphWindow{" +
                "lines=" + Arrays.toString(lines) +
                ", bounds=" + bounds +
                ", xValues=" + Arrays.toString(xValues) +
                ", yValues=" + Arrays.toString(yValues) +
                ", yTopExtremum=" + yTopExtremum +
                ", yLowExtremum=" + yLowExtremum +
                '}';
    }
}
