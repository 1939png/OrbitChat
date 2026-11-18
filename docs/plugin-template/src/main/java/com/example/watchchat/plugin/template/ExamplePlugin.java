package com.example.watchchat.plugin.template;

import java.util.Collections;
import java.util.Set;

/** Replace this example with a real plugin implementation. */
public final class ExamplePlugin implements Plugin {
    public String id() { return "example.greeting"; }
    public String displayName() { return "Greeting example"; }
    public Set<String> capabilities() { return Collections.singleton("chat.message"); }
    public PluginResult execute(PluginContext context, String input) {
        if (context == null) return PluginResult.error("context required");
        if (input == null || input.trim().isEmpty()) return PluginResult.error("input required");
        context.postUserVisibleMessage("Hello, " + input.trim());
        return PluginResult.ok();
    }
}
