package com.example.watchchat.plugin;
/** Receives isolated plugin failures. */
public interface PluginErrorHandler { void onError(ChatPlugin plugin,String operation,Throwable error); PluginErrorHandler NO_OP=new PluginErrorHandler(){public void onError(ChatPlugin p,String o,Throwable e){}}; }
