package com.example.watchchat.data;

import android.content.Context;
import android.content.SharedPreferences;

/** API credentials are stored only in this app's private preferences. */
public final class ChatConfigStore {
    private static final String PREFS="chat_config_private"; private final SharedPreferences prefs; private final Context context;
    public ChatConfigStore(Context context){this.context=context.getApplicationContext();prefs=this.context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public void save(ChatConfig c){prefs.edit().putString("provider",c.provider.name()).putString("baseUrl",c.baseUrl).putString("apiKey",c.apiKey).putString("model",c.model).putInt("connect",c.connectTimeoutMs).putInt("read",c.readTimeoutMs).putInt("retries",c.maxRetries).apply();}
    public ChatConfig load(){
        try{
            com.example.watchchat.data.ProviderStore.migrateLegacy(context);
            com.example.watchchat.data.ProviderStore.Provider p=com.example.watchchat.data.ProviderStore.activeProvider(context);
            if(p!=null)return new ChatConfig(ChatConfig.Provider.valueOf(com.example.watchchat.data.ProviderStore.normalizeProtocol(p.protocol)),p.baseUrl,p.apiKey,com.example.watchchat.data.ProviderStore.activeModel(context),30000,60000,0);
        }catch(Throwable ignored){}
        try{
            String provider=prefs.getString("provider",ChatConfig.Provider.OPENAI.name());
            ChatConfig.Provider proto;
            try{proto=ChatConfig.Provider.valueOf(provider);}catch(Throwable t){proto=ChatConfig.Provider.OPENAI;}
            return new ChatConfig(proto,prefs.getString("baseUrl","https://api.openai.com/v1"),prefs.getString("apiKey",""),prefs.getString("model","gpt-4o-mini"),prefs.getInt("connect",15000),prefs.getInt("read",60000),prefs.getInt("retries",1));
        }catch(Throwable t){
            return new ChatConfig(ChatConfig.Provider.OPENAI,"https://api.openai.com/v1","","gpt-4o-mini",15000,60000,1);
        }
    }
    public int textScale(){return prefs.getInt("textScale",1);}
    public void setTextScale(int value){prefs.edit().putInt("textScale",Math.max(0,Math.min(2,value))).apply();}
    public void applyPreferences(){SharedPreferences ui=context.getSharedPreferences("chat_settings_ui",Context.MODE_PRIVATE);ChatConfig current=load();String provider=ui.getString("provider",current.provider.name());String url=ui.getString("baseUrl",current.baseUrl);String key=ui.getString("apiKey",current.apiKey);String model=ui.getString("model",current.model);save(new ChatConfig(ChatConfig.Provider.valueOf(provider),url,key,model,current.connectTimeoutMs,current.readTimeoutMs,current.maxRetries));setTextScale(parseScale(ui.getString("textScaleChoice",String.valueOf(textScale()))));setHighContrast(ui.getBoolean("highContrast",highContrast()));}
    private int parseScale(String value){try{return Integer.parseInt(value);}catch(Exception ignored){return 1;}}
    public boolean hasConfig(){return !prefs.getString("apiKey","").trim().isEmpty();}
    public boolean highContrast(){return prefs.getBoolean("highContrast",false);}
    public void setHighContrast(boolean value){prefs.edit().putBoolean("highContrast",value).apply();}
    public void clear(){prefs.edit().clear().apply();}
}
