package com.example.watchchat.plugin;
import java.util.ArrayList;import java.util.Collections;import java.util.LinkedHashMap;import java.util.List;import java.util.Map;
/** Thread-safe registry; every plugin callback is isolated. */
public final class PluginRegistry {
 private final Map<String,ChatPlugin> plugins=new LinkedHashMap<>(); private final PluginContext context; private final PluginErrorHandler errors; private boolean started;
 public PluginRegistry(PluginContext context,PluginErrorHandler errors){this.context=context;this.errors=errors==null?PluginErrorHandler.NO_OP:errors;}
 public synchronized boolean register(final ChatPlugin plugin){if(plugin==null||plugin.getMetadata()==null)throw new IllegalArgumentException("plugin/metadata required");String id=plugin.getMetadata().getId();if(plugins.containsKey(id))return false;plugins.put(id,plugin);invoke(plugin,"onInstall",new Action(){public void run(){plugin.onInstall(context);}});if(started)invoke(plugin,"onStart",new Action(){public void run(){plugin.onStart(context);}});return true;}
 public synchronized boolean unregister(final String id){ChatPlugin p=plugins.remove(id);if(p==null)return false;invoke(p,"onStop",new Action(){public void run(){p.onStop(context);}});return true;}
 public synchronized void startAll(){if(started)return;started=true;for(final ChatPlugin p:snapshot())invoke(p,"onStart",new Action(){public void run(){p.onStart(context);}});}
 public synchronized void stopAll(){if(!started)return;started=false;for(final ChatPlugin p:snapshot())invoke(p,"onStop",new Action(){public void run(){p.onStop(context);}});}
 public synchronized List<PluginMetadata> listMetadata(){List<PluginMetadata> r=new ArrayList<>();for(ChatPlugin p:snapshot())r.add(p.getMetadata());return Collections.unmodifiableList(r);}
 public PluginResult dispatchMessage(MessageContext c,String message){for(ChatPlugin p:snapshot()){MessageHandler h;try{h=p.getMessageHandler();}catch(Throwable e){report(p,"getMessageHandler",e);continue;}if(h==null)continue;try{PluginResult r=h.onMessage(c,message);if(r!=null&&r.isHandled())return r;}catch(Throwable e){report(p,"onMessage",e);}}return PluginResult.notHandled();}
 public synchronized List<ModelProvider> listModelProviders(){List<ModelProvider> r=new ArrayList<>();for(ChatPlugin p:snapshot())try{ModelProvider m=p.getModelProvider();if(m!=null)r.add(m);}catch(Throwable e){report(p,"getModelProvider",e);}return Collections.unmodifiableList(r);}
 private synchronized List<ChatPlugin> snapshot(){return new ArrayList<>(plugins.values());} private void invoke(ChatPlugin p,String op,Action a){try{a.run();}catch(Throwable e){report(p,op,e);}} private void report(ChatPlugin p,String op,Throwable e){try{errors.onError(p,op,e);}catch(Throwable ignored){}}
 private interface Action{void run();}
}
