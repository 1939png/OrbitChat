package com.example.watchchat;

import com.example.watchchat.data.ChatConfig;
import com.example.watchchat.data.ModelCapabilities;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Every picker value must reach the payload verbatim. */
public final class ReasoningPayloadTest {
    private ChatConfig cfg() {
        return new ChatConfig(ChatConfig.Provider.OPENAI, "https://example.test/v1", "k", "m", 1000, 1000, 1);
    }

    @Test public void fivePickerValuesProduceFiveSuffixes() throws Exception {
        String[] expect = {"none", "low", "high", "ultra", "max"};
        for (String v : expect) {
            JSONObject p = new JSONObject();
            ModelCapabilities.forConfig(cfg(), null).apply(p, v);
            assertEquals(v, p.optString("reasoning_effort"));
        }
    }

    @Test public void customSuffixIsApplied() throws Exception {
        JSONObject p = new JSONObject();
        ModelCapabilities.forConfig(cfg(), Arrays.asList("minimal", "extreme")).apply(p, "minimal");
        assertEquals("minimal", p.optString("reasoning_effort"));
    }

    @Test public void noneDisablesThinkingObjectToo() throws Exception {
        JSONObject p = new JSONObject();
        ModelCapabilities.forConfig(cfg(), null).apply(p, "none");
        assertEquals("disabled", p.getJSONObject("thinking").optString("type"));
    }

    @Test public void lowEnablesThinkingObject() throws Exception {
        JSONObject p = new JSONObject();
        ModelCapabilities.forConfig(cfg(), null).apply(p, "low");
        assertEquals("enabled", p.getJSONObject("thinking").optString("type"));
    }

    @Test public void emptyValueAddsNothing() throws Exception {
        JSONObject p = new JSONObject();
        ModelCapabilities.forConfig(cfg(), null).apply(p, "");
        assertFalse(p.has("reasoning_effort"));
    }
}
