package com.almadevelop.telegram.chart.graph;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.List;

public class Graph {
    @NonNull
    private final GraphAxis xAxis;
    @NonNull
    private final List<GraphLine> lines;

    public Graph(@NonNull GraphAxis xAxis, @NonNull List<GraphLine> lines) {
        if (xAxis == null) {
            throw new IllegalArgumentException("X axis cannot be null");
        }
        if (lines == null) {
            throw new IllegalArgumentException("Lines cannot be null");
        }

        this.xAxis = xAxis;
        this.lines = lines;
    }

    @NonNull
    public GraphAxis getXAxis() {
        return xAxis;
    }

    @NonNull
    public List<GraphLine> getLines() {
        return lines;
    }

    /**
     * @return number of pairs (x, y) in the Graph
     */
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int size() {
        return xAxis.getData().size();
    }

    /**
     * @return number of lines in the graph
     */
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int linesCount() {
        return lines.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Graph graph = (Graph) o;

        if (!xAxis.equals(graph.xAxis)) {
            return false;
        }
        return lines.equals(graph.lines);
    }

    @Override
    public int hashCode() {
        int result = xAxis.hashCode();
        result = 31 * result + lines.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "xAxis=" + xAxis +
                ", lines=" + lines +
                '}';
    }
}
