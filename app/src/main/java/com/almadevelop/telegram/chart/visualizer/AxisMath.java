package com.almadevelop.telegram.chart.visualizer;

/**
 * Helper class to convert from axis values to pixels and vice versa
 */
class AxisMath {
    private float start;
    private float end;

    private long topValue;
    private long lowValue;

    private int pointsCount;

    /**
     * Set axis length
     *
     * @param start axis start (pixels)
     * @param end   axis end (pixels)
     */
    void setSize(float start, float end) {

        this.start = start;
        this.end = end;
    }

    /**
     * Set axis values
     *
     * @param topValue    last axis value
     * @param lowValue    first axis value
     * @param pointsCount number of points at the axis
     */
    void setValues(long topValue, long lowValue, int pointsCount) {
        this.topValue = topValue;
        this.lowValue = lowValue;

        this.pointsCount = pointsCount;
    }

    float length() {
        return end - start;
    }

    /**
     * Convert provided value to it pixel position at the Axis
     *
     * @param value value to convert
     * @return pixel coordinate of the value
     */
    float valueToPixel(long value) {
        return (((float) value - (float) lowValue) * currentPixelPerValue())+start;
    }

    /**
     * Convert pixel coordinate to it axis value
     *
     * @param pixel pixel coordinate to convert from
     * @return axis value
     */
    long pixelToValue(float pixel) {
        return (long) (checkPixel(pixel) * (topValue - lowValue) / length()) + lowValue;
    }

    float pixelPerPoint() {
        return length() / (float) (pointsCount - 1);
    }

    /**
     * Convert provided pixel position to the point position at the axis
     *
     * @param pixel pixel value to convert
     * @return
     */
    float pixelToPointPosition(float pixel) {
        return checkPixel(pixel) / pixelPerPoint();
    }

    int pixelToRoundPointPosition(float pixel) {
        return Math.round(pixelToPointPosition(pixel));
    }

    /**
     * Return how much pixels in each axis value.
     * Using current axis length and extremums
     *
     * @return pixel per single Axis value
     */
    float currentPixelPerValue() {
        return pixelPerValue(topValue, lowValue);
    }

    /**
     * Return how much pixels in each axis value if that axis has provided top and low values
     *
     * @param topValue last value
     * @param lowValue first value
     * @return
     */
    float pixelPerValue(long topValue, long lowValue) {
        return length() / (float) (topValue - lowValue);
    }

    private float checkPixel(float pixel) {
        return pixel - start;
    }
}
