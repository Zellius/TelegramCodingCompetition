package com.almadevelop.telegram.chart.parser;

import android.support.annotation.NonNull;
import android.util.JsonReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class NamesParser {
    private NamesParser() {
    }

    static Map<String, String> parse(@NonNull JsonReader reader) throws IOException {
        final Map<String, String> names = new HashMap<>();

        reader.beginObject();

        readerLooper:
        while (reader.hasNext()) {
            switch (reader.peek()) {
                case NAME:
                    names.put(reader.nextName(), reader.nextString());
                    break;
                case END_OBJECT:
                    break readerLooper;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return names;
    }
}
