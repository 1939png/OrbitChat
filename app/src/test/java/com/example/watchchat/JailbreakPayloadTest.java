package com.example.watchchat;

import com.example.watchchat.data.Attachment;
import com.example.watchchat.data.ChatConfig;
import com.example.watchchat.data.ChatMessage;
import com.example.watchchat.data.RequestOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Standalone check: jailbreak text lands in the outgoing payload for both providers. */
public final class JailbreakPayloadTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        Method payload=Class.forName("com.example.watchchat.data.ChatClient")
            .getDeclaredMethod("payload",List.class,ChatConfig.class,RequestOptions.class,List.class,String.class);
        payload.setAccessible(true);
        List<ChatMessage> history=new ArrayList<>();
        history.add(ChatMessage.of(ChatMessage.Role.USER,"hello"));
        ChatConfig openai=new ChatConfig(ChatConfig.Provider.OPENAI,"https://api.example.com/v1","sk-test","gpt-test",30000,60000,0);
        String jb="测试注入标记INJECTION_MARKER";
        JSONObject body=(JSONObject)payload.invoke(null,history,openai,RequestOptions.defaults(),new ArrayList<Attachment>(),jb);
        JSONArray messages=body.getJSONArray("messages");
        check(messages.length()==2,"openai injected as extra message");
        check("system".equals(messages.getJSONObject(0).getString("role")),"first is system");
        check(messages.getJSONObject(0).getString("content").contains("INJECTION_MARKER"),"marker present");
        JSONObject off=(JSONObject)payload.invoke(null,history,openai,RequestOptions.defaults(),new ArrayList<Attachment>(),"");
        check(off.getJSONArray("messages").length()==1,"empty jailbreak sends nothing");
        ChatConfig anthropic=new ChatConfig(ChatConfig.Provider.ANTHROPIC,"https://api.example.com","sk-test","claude-test",30000,60000,0);
        JSONObject ab=(JSONObject)payload.invoke(null,history,anthropic,RequestOptions.defaults(),new ArrayList<Attachment>(),jb);
        check(ab.getString("system").contains("INJECTION_MARKER"),"anthropic system field");
        System.out.println("PASS jailbreak payload checks");
    }
}
