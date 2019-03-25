package com.almadevelop.telegram.chart.parser;

import android.support.annotation.NonNull;
import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class ColumnsParser {
    private ColumnsParser() {
    }

    static List<ParsedColumnData> parse(@NonNull JsonReader reader) throws IOException {
        final List<ParsedColumnData> columnsData = new ArrayList<>();

        reader.beginArray();

        readerLoopper:
        while (reader.hasNext()) {
            switch (reader.peek()) {
                case BEGIN_ARRAY:
                    columnsData.add(parseSingleColumn(reader));
                    break;
                case END_ARRAY:
                    break readerLoopper;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endArray();

        return columnsData;
    }

    private static ParsedColumnData parseSingleColumn(@NonNull JsonReader reader) throws IOException {
        String columnLabel = null;
        final List<Long> columnValues = new ArrayList<>();

        long topPeak = 0;
        long lowerPeak = Long.MAX_VALUE;

        reader.beginArray();

        readerLoopper:
        while (reader.hasNext()) {
            switch (reader.peek()) {
                case NUMBER: {
                    final long value = reader.nextLong();

                    topPeak = Math.max(topPeak, value);
                    lowerPeak = Math.min(lowerPeak, value);

                    columnValues.add(value);
                    break;
                }
                case STRING:
                    if (columnLabel != null) {
                        throw new IllegalChartJsonFormat("Column name appeared second time");
                    }
                    columnLabel = reader.nextString();
                    break;
                case END_ARRAY:
                    break readerLoopper;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endArray();

        if (columnLabel == null) {
            throw new IllegalChartJsonFormat("The column doesn't contains label");
        }

        return new ParsedColumnData(columnLabel, columnValues, topPeak, lowerPeak);
    }

    static final class ParsedColumnData {
        @NonNull
        private final String label;
        @NonNull
        private final List<Long> values;
        private final long topPeak;
        private final long lowerPeak;

        private ParsedColumnData(@NonNull String label, @NonNull List<Long> values, long topPeak, long lowerPeak) {
            this.label = label;
            this.values = values;
            this.topPeak = topPeak;
            this.lowerPeak = lowerPeak;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        @NonNull
        public List<Long> getValues() {
            return values;
        }

        public long getTopPeak() {
            return topPeak;
        }

        public long getLowerPeak() {
            return lowerPeak;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ParsedColumnData that = (ParsedColumnData) o;

            if (topPeak != that.topPeak) {
                return false;
            }
            if (lowerPeak != that.lowerPeak) {
                return false;
            }
            if (!label.equals(that.label)) {
                return false;
            }
            return values.equals(that.values);
        }

        @Override
        public int hashCode() {
            int result = label.hashCode();
            result = 31 * result + values.hashCode();
            result = 31 * result + (int) (topPeak ^ (topPeak >>> 32));
            result = 31 * result + (int) (lowerPeak ^ (lowerPeak >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "ParsedColumnData{" +
                    "label='" + label + '\'' +
                    ", values=" + values +
                    ", topPeak=" + topPeak +
                    ", lowerPeak=" + lowerPeak +
                    '}';
        }
    }
}
