package com.example.watchchat.plugin;
/** Message handling result. */
public final class PluginResult { private static final PluginResult NOT_HANDLED=new PluginResult(false,null); private final boolean handled; private final String response; private PluginResult(boolean h,String r){handled=h;response=r;} public static PluginResult notHandled(){return NOT_HANDLED;} public static PluginResult handled(String response){return new PluginResult(true,response);} public boolean isHandled(){return handled;} public String getResponse(){return response;} }
