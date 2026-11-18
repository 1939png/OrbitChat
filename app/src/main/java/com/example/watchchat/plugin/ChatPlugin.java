package com.example.watchchat.plugin;
/** Stable plugin contract with lifecycle and optional extensions. */
public interface ChatPlugin { PluginMetadata getMetadata(); default void onInstall(PluginContext context){} default void onStart(PluginContext context){} default void onStop(PluginContext context){} default MessageHandler getMessageHandler(){return null;} default ModelProvider getModelProvider(){return null;} }
