package com.almadevelop.telegram.chart.visualizer;

class AxisScale {
    private static final int TYPE_KNOWN_SIZE = 0;
    private static final int TYPE_CALCULATABLE = 1;

    private final int type;

    private float size = Float.MIN_VALUE;
    private float sizeFactor = Float.MIN_VALUE;

    private int count;

    private long maxValue;
    private long minValue;

    private long thick;

    private long min;
    private long max;

    private AxisScale(float size, float sizeFactor, int type) {
        this.type = type;
        this.size = size;
        this.sizeFactor = sizeFactor;
    }

    /**
     * Calculate scales count and recalculate min and max values if they were set before
     *
     * @param length a length of axis
     */
    void setAxisLength(float length) {
        if (type == TYPE_CALCULATABLE) {
            size = length * sizeFactor;
        }

        count = Math.round(length / size);

        if (maxValue != 0 && minValue != 0) {
            setAxisMinMax(maxValue, minValue);
        } else {
            thick = 0;
        }
    }

    static AxisScale knownSizeScale(float size) {
        return new AxisScale(size, Float.MIN_VALUE, TYPE_KNOWN_SIZE);
    }

    static AxisScale calculatableSizeScale(float sizeFactor) {
        return new AxisScale(Float.MIN_VALUE, sizeFactor, TYPE_CALCULATABLE);
    }

    public long getThick() {
        return thick;
    }

    int getCount() {
        return count;
    }

    float getSize() {
        return size;
    }

    /**
     * Returns a "nice" number approximately equal to range Rounds
     * the number if round = true Takes the ceiling if round = false.
     *
     * @param range the data range
     * @param round whether to round the result
     * @return a "nice" number to be used for the data range
     */
    private static double niceNum(double range, boolean round) {
        //exponent of range
        final double exponent = Math.floor(Math.log10(range));
        //fractional part of range
        final double fraction = range / Math.pow(10, exponent);

        //nice, rounded fraction
        final double niceFraction;

        if (round) {
            if (fraction < 1.5) {
                niceFraction = 1;
            } else if (fraction < 3) {
                niceFraction = 2;
            } else if (fraction < 7) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        } else {
            if (fraction <= 1) {
                niceFraction = 1;
            } else if (fraction <= 2) {
                niceFraction = 2;
            } else if (fraction <= 5) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        }

        return niceFraction * Math.pow(10, exponent);
    }

    void setAxisMinMax(long maxValue, long minValue) {
        if (count <= 0) {
            throw new IllegalStateException("Number of axis columns cannot be zero or negative");
        }

        if (this.maxValue == maxValue && this.minValue == minValue) {
            return;
        }

        this.maxValue = maxValue;
        this.minValue = minValue;

        final double range = niceNum(maxValue - minValue, false);
        final double tickSpacing = niceNum(range / (count - 1), true);

        this.min = (long) (Math.floor(minValue / tickSpacing) * tickSpacing);
        this.max = (long) (Math.ceil(maxValue / tickSpacing) * tickSpacing);
        this.thick = Math.round(tickSpacing);
    }

    long getScaleByPosition(int pos) {
        return max / count * pos + min;
    }
}
