package com.example.watchchat;

import com.example.watchchat.data.ChatConfig;
import com.example.watchchat.data.ModelCapabilities;
import java.util.Arrays;

/** Standalone capability checks; android.jar supplies JSON class signatures only. */
public final class ModelCapabilitiesTest {
    private static int checks;
    private static void check(boolean ok) { if (!ok) throw new AssertionError("check " + checks); checks++; }
    private static ModelCapabilities capability(ChatConfig.Provider provider, String host, String model) {
        return ModelCapabilities.forConfig(new ChatConfig(provider, host, "", model, 1000, 1000, 0));
    }
    public static void main(String[] args) {
        for (String host : new String[]{"https://api.deepseek.com", "https://third-party.example/v1", "http://localhost:8080/v1"}) {
            ModelCapabilities c = capability(ChatConfig.Provider.OPENAI, host, "deepseek-flash");
            check(Arrays.equals(c.labels(), new String[]{"NONE", "LOW", "HIGH", "MAX"}));
            check(Arrays.equals(c.values(), new String[]{"none", "low", "high", "max"}));
            check(c.supports("none") && c.supports("low") && c.supports("high") && c.supports("max"));
            check(!c.supports("medium") && !c.supports(""));
        }
        check(capability(ChatConfig.Provider.ANTHROPIC, "https://api.deepseek.com", "deepseek-flash").labels().length == 0);
        check(capability(ChatConfig.Provider.OPENAI, "https://api.deepseek.com", "deepseek-flash-extra").labels().length == 0);
        check(Arrays.equals(capability(ChatConfig.Provider.OPENAI, "https://api.openai.com/v1", "o3").values(), new String[]{"low", "medium", "high"}));
        check(capability(ChatConfig.Provider.OPENAI, "https://third-party.example", "o3").labels().length == 0);
        check(Arrays.equals(capability(ChatConfig.Provider.ANTHROPIC, "https://api.anthropic.com/v1", "claude-sonnet-4-20250514").values(), new String[]{"1024", "2048", "4096"}));
        System.out.println("PASS " + checks + " model capability checks");
    }
}
