/*
 * Copyright (c) 2020 Castle
 */

package com.sample.demo_keystore.castle.queue;

import com.sample.demo_keystore.castle.Utils;
import com.squareup.tape2.ObjectQueue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

class GsonConverter<T> implements ObjectQueue.Converter<T> {
    private final Class<T> type;

    GsonConverter(Class<T> type) {
        this.type = type;
    }

    @Override
    public T from(byte[] bytes) {
        try {
            Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
            return Utils.getGsonInstance().fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void toStream(T object, OutputStream bytes) throws IOException {
        Writer writer = new OutputStreamWriter(bytes);
        Utils.getGsonInstance().toJson(object, writer);
        writer.close();
    }
}
