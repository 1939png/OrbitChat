package com.example.watchchat;

import com.example.watchchat.data.ChatUsage;

/** Standalone checks: javac ChatUsage.java ChatUsageTest.java; java com.example.watchchat.ChatUsageTest */
public final class ChatUsageTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        ChatUsage openai=ChatUsage.partial(false,120,null,null,null);
        check(openai.contextTokens==120&&openai.inputTokens==120,"openai input");
        check(openai.cacheHit==null,"missing cache is unknown");
        ChatUsage deepseek=ChatUsage.partial(false,100,null,60,null);
        check(deepseek.cacheHit.booleanValue(),"cache hit true");
        check(deepseek.contextTokens==100,"context excludes cached");
        ChatUsage anthropic=ChatUsage.partial(true,80,null,30,10);
        check(anthropic.inputTokens==120,"anthropic total input");
        check(anthropic.contextTokens==120,"anthropic context");
        ChatUsage merged=openai.merge(ChatUsage.partial(false,null,55,null,null));
        check(merged.inputTokens==120&&merged.outputTokens==55,"partial merge keeps old");
        ChatUsage again=merged.merge(ChatUsage.partial(false,130,60,null,null));
        check(again.inputTokens==130&&again.outputTokens==60,"later full overrides");
        check(ChatUsage.unknown().cacheHit==null&&ChatUsage.unknown().inputTokens==null,"unknown");
        System.out.println("PASS ChatUsage checks");
    }
}
