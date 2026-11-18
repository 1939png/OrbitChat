package com.example.watchchat.plugin;

import com.example.watchchat.plugin.example.EchoPlugin;
import java.util.ArrayList;
import java.util.List;

/** Standalone checks: compile plugin sources and run this class. */
public final class PluginRegistryTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        PluginRegistry registry=new PluginRegistry(null,PluginErrorHandler.NO_OP);
        check(registry.register(new EchoPlugin()),"register");
        check(!registry.register(new EchoPlugin()),"duplicate id");
        PluginResult result=registry.dispatchMessage(new MessageContext("c1",null),"/echo hello");
        check(result.isHandled()&&"hello".equals(result.getResponse()),"echo");
        check(!registry.dispatchMessage(new MessageContext("c1",null),"hello").isHandled(),"unhandled");
        final List<String> errors=new ArrayList<String>();
        PluginRegistry isolated=new PluginRegistry(null,new PluginErrorHandler(){public void onError(ChatPlugin p,String op,Throwable e){errors.add(op);}});
        isolated.register(new ChatPlugin(){public PluginMetadata getMetadata(){return new PluginMetadata("bad","Bad","1","");}public MessageHandler getMessageHandler(){throw new RuntimeException("boom");}});
        isolated.register(new EchoPlugin());
        check(isolated.dispatchMessage(new MessageContext("c1",null),"/echo ok").isHandled(),"isolation");
        check(errors.size()==1,"error callback");
        System.out.println("PASS plugin registry checks");
    }
}
