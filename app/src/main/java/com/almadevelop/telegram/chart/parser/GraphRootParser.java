package com.almadevelop.telegram.chart.parser;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.JsonReader;

import com.almadevelop.telegram.chart.graph.Graph;
import com.almadevelop.telegram.chart.graph.GraphAxis;
import com.almadevelop.telegram.chart.graph.GraphLine;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.almadevelop.telegram.chart.parser.Constants.COLUMN_TYPE_LINE;
import static com.almadevelop.telegram.chart.parser.Constants.COLUMN_TYPE_X;

/**
 * Contains function to parse graph json
 */
public final class GraphRootParser {
    private GraphRootParser() {
    }

    /**
     * Parse json from {@link Reader} and return list of parsed graphs
     *
     * @param in contains graph json
     * @throws IOException            see {@link JsonReader} docs
     * @throws IllegalChartJsonFormat in case of invalid json format
     */
    public static List<Graph> parse(@NonNull Reader in) throws IOException {
        final JsonReader reader = new JsonReader(in);

        final List<Graph> graphs = new ArrayList<>();

        try {
            reader.beginArray();

            readerLooper:
            while (reader.hasNext()) {
                switch (reader.peek()) {
                    case END_ARRAY:
                        break readerLooper;
                    case BEGIN_OBJECT:
                        graphs.add(parseSingleGraph(reader));
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }

            reader.endArray();
        } finally {
            reader.close();
        }

        return graphs;
    }

    /**
     * Parse single graph object from the list of graphs
     *
     * @param reader input JSON reader
     * @throws IOException
     */
    private static Graph parseSingleGraph(@NonNull JsonReader reader) throws IOException {
        final GraphBuilder graphBuilder = new GraphBuilder();

        reader.beginObject();

        readerLooper:
        while (reader.hasNext()) {
            switch (reader.peek()) {
                case NAME:
                    switch (reader.nextName()) {
                        case "columns":
                            graphBuilder.setColumns(ColumnsParser.parse(reader));
                            break;
                        case "types":
                            graphBuilder.setTypes(TypesParser.parse(reader));
                            break;
                        case "names":
                            graphBuilder.setNames(NamesParser.parse(reader));
                            break;
                        case "colors":
                            graphBuilder.setColors(ColorParser.parse(reader));
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                    break;
                case END_OBJECT:
                    break readerLooper;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return graphBuilder.build();
    }

    /**
     * Graph builderfrom parser result
     */
    private final static class GraphBuilder {
        private List<ColumnsParser.ParsedColumnData> columns;
        private Map<String, Integer> types;
        private Map<String, String> names;
        private Map<String, Integer> colors;

        void setColumns(@NonNull List<ColumnsParser.ParsedColumnData> columns) {
            if (columns == null) {
                throw new IllegalArgumentException("Columns cannot be null");
            }
            this.columns = columns;
        }

        void setTypes(@NonNull Map<String, Integer> types) {
            if (types == null) {
                throw new IllegalArgumentException("Types cannot be null");
            }
            this.types = types;
        }

        void setNames(@NonNull Map<String, String> names) {
            if (names == null) {
                throw new IllegalArgumentException("Names cannot be null");
            }
            this.names = names;
        }

        void setColors(@NonNull Map<String, Integer> colors) {
            if (colors == null) {
                throw new IllegalArgumentException("Colors cannot be null");
            }
            this.colors = colors;
        }

        /**
         * Build {@link com.almadevelop.telegram.chart.graph.Graph}
         */
        Graph build() {
            final List<GraphLine> graphLines = new ArrayList<>();
            GraphAxis xAxis = null;

            for (ColumnsParser.ParsedColumnData columnData : columns) {
                final String columnLabel = columnData.getLabel();
                final List<Long> columnValues = columnData.getValues();

                //Should return non null type
                //noinspection ConstantConditions
                switch (types.get(columnLabel)) {
                    case COLUMN_TYPE_X:
                        xAxis = new GraphAxis(columnValues, columnData.getTopPeak(), columnData.getLowerPeak());
                        break;
                    case COLUMN_TYPE_LINE: {
                        final String lineName = names.get(columnLabel);
                        //Should return non null color
                        //noinspection ConstantConditions
                        @ColorInt final int lineColor = colors.get(columnLabel);

                        graphLines.add(new GraphLine(columnLabel, lineName, columnValues, columnData.getTopPeak(), columnData.getLowerPeak(), lineColor));
                        break;
                    }
                }
            }

            return new Graph(xAxis, graphLines);
        }
    }
}
