package com.almadevelop.telegram.chart.parser;

import android.support.annotation.NonNull;
import android.util.JsonReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.almadevelop.telegram.chart.parser.Constants.COLUMN_TYPE_LINE;
import static com.almadevelop.telegram.chart.parser.Constants.COLUMN_TYPE_X;

final class TypesParser {
    private TypesParser() {
    }

    static Map<String, Integer> parse(@NonNull JsonReader reader) throws IOException {
        final Map<String, Integer> types = new HashMap<>();

        reader.beginObject();

        while (reader.hasNext()) {
            switch (reader.peek()) {
                case NAME: {
                    final String columnName = reader.nextName();
                    final String columnType = reader.nextString();

                    switch (columnType) {
                        case "x":
                            types.put(columnName, COLUMN_TYPE_X);
                            break;
                        case "line":
                            types.put(columnName, COLUMN_TYPE_LINE);
                            break;
                        default:
                            throw new IllegalChartJsonFormat("Unsupported column type: ".concat(columnType));
                    }

                    break;
                }
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return types;
    }
}
