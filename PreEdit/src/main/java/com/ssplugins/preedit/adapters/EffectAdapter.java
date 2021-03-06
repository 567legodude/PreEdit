package com.ssplugins.preedit.adapters;

import com.google.gson.*;
import com.ssplugins.preedit.edit.Catalog;
import com.ssplugins.preedit.edit.Effect;
import com.ssplugins.preedit.exceptions.SimpleException;

import java.lang.reflect.Type;

public class EffectAdapter implements JsonSerializer<Effect>, JsonDeserializer<Effect> {
    
    private Catalog catalog;
    
    public EffectAdapter(Catalog catalog) {
        this.catalog = catalog;
    }
    
    @Override
    public Effect deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject json = element.getAsJsonObject();
        String name = json.get("name").getAsString();
        Effect effect = catalog.createEffect(name).orElseThrow(() -> new JsonParseException(new SimpleException("Template uses effect \"" + name + "\" which is missing.")));
        if (json.has("displayName")) {
            JsonElement display = json.get("displayName");
            if (!display.isJsonNull()) effect.setDisplayName(display.getAsString());
        }
        JsonObject inputs = json.getAsJsonObject("inputs");
        InputMapAdapter.deserialize(inputs, effect.getInputs());
        return effect;
    }
    
    @Override
    public JsonElement serialize(Effect effect, Type type, JsonSerializationContext context) {
        JsonObject out = new JsonObject();
        out.addProperty("name", effect.getName());
        out.addProperty("displayName", effect.getDisplayName());
        out.add("inputs", context.serialize(effect.getInputs()));
        return out;
    }
    
}
