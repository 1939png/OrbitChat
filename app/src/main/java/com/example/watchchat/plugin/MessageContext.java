package com.example.watchchat.plugin;
/** Message context kept independent from Activity and ChatClient. */
public final class MessageContext { private final String conversationId; private final PluginContext pluginContext; public MessageContext(String id,PluginContext context){conversationId=id==null?"":id;pluginContext=context;} public String getConversationId(){return conversationId;} public PluginContext getPluginContext(){return pluginContext;} }
