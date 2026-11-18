package com.example.watchchat;
import java.util.List;
public final class ChatModels {
 private ChatModels(){}
 public static final class Message {public final String id,role,text;public final long timestamp;public final boolean streaming;public Message(String i,String r,String t,long ts,boolean s){id=i;role=r;text=t;timestamp=ts;streaming=s;}}
 public static final class Conversation {public final String id,title;public final long updatedAt;public Conversation(String i,String t,long u){id=i;title=t;updatedAt=u;}}
 public static final class ProviderConfig {public final String provider,baseUrl,apiKey,model;public ProviderConfig(String p,String u,String k,String m){provider=p;baseUrl=u;apiKey=k;model=m;}}
 public interface StreamListener {void onText(String delta);void onComplete();void onError(String message);}
 public interface ChatCallbacks {List<Conversation> loadConversations();List<Message> loadMessages(String id);void sendText(String id,String text,StreamListener listener);void stopGeneration(String id);void saveConfig(ProviderConfig config);}
}
