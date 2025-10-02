/*
 * Copyright (c) 2020 Castle
 */

package com.sample.demo_keystore.castle;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.sample.demo_keystore.castle.api.model.CustomEvent;
import com.sample.demo_keystore.castle.api.model.Event;
import com.sample.demo_keystore.castle.api.model.ScreenEvent;

import java.lang.reflect.Type;

class EventAdapter implements JsonDeserializer<Event> {

    private static final Gson gson = new Gson();

    @Override
    public Event deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (typeOfT.equals(Event.class)) {
            String type = json.getAsJsonObject().get("type").getAsString();

            switch (type) {
                case Event.EVENT_TYPE_SCREEN:
                    typeOfT = ScreenEvent.class;
                    break;
                case Event.EVENT_TYPE_CUSTOM:
                    typeOfT = CustomEvent.class;
                    break;
            }
        }

        return gson.fromJson(json, typeOfT);
    }
}
