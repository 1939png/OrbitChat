package com.example.watchchat.plugin.example;
import com.example.watchchat.plugin.*;
/** Minimal runnable example: /echo hello returns hello. */
public final class EchoPlugin implements ChatPlugin { private static final PluginMetadata META=new PluginMetadata("example.echo","Echo Example","1.0.0","Replies to /echo commands"); public PluginMetadata getMetadata(){return META;} public MessageHandler getMessageHandler(){return new MessageHandler(){public PluginResult onMessage(MessageContext c,String m){if(m==null||!m.startsWith("/echo"))return PluginResult.notHandled();return PluginResult.handled(m.length()<=5?"":m.substring(5).trim());}};} }
