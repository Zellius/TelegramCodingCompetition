package com.almadevelop.telegram.chart.parser;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.JsonReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class ColorParser {
    private ColorParser() {
    }

    static Map<String, Integer> parse(@NonNull JsonReader reader) throws IOException {
        final Map<String, Integer> colors = new HashMap<>();

        reader.beginObject();

        readerLooper:
        while (reader.hasNext()) {
            switch (reader.peek()) {
                case NAME:
                    colors.put(reader.nextName(), Color.parseColor(reader.nextString()));
                    break;
                case END_OBJECT:
                    break readerLooper;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return colors;
    }
}
