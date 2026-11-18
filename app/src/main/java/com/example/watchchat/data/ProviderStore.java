package com.example.watchchat.data;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Multi-provider registry with per-provider model lists; unlimited entries. */
public final class ProviderStore {
    public static final class Provider {
        public final String id,displayName,baseUrl,protocol,apiKey;
        public final List<String> reasoningOptions;
        public final List<String> models;
        public Provider(String id,String displayName,String baseUrl,String protocol,String apiKey,List<String> models){this(id,displayName,baseUrl,protocol,apiKey,models,null);}
        public Provider(String id,String displayName,String baseUrl,String protocol,String apiKey,List<String> models,List<String> reasoning){this.id=id;this.displayName=displayName;this.baseUrl=baseUrl;this.protocol=protocol;this.apiKey=apiKey;this.models=models==null?new ArrayList<>():models;this.reasoningOptions=reasoning==null?new ArrayList<>():reasoning;}
    }
    private static final String PREFS="providers_store";
    private static final String KEY_LIST="providers";
    private static final String KEY_ACTIVE="active_model_key";
    private ProviderStore(){}

    public static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}

    public static synchronized List<Provider> list(Context c){
        List<Provider> out=new ArrayList<>();
        try{JSONArray a=new JSONArray(prefs(c).getString(KEY_LIST,"[]"));
            for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);
                List<String> models=new ArrayList<>();
                JSONArray ms=o.optJSONArray("models");
                if(ms!=null)for(int k=0;k<ms.length();k++){String m=ms.optString(k);if(!m.isEmpty())models.add(m);}
                List<String> ro=new ArrayList<>();
                JSONArray rs=o.optJSONArray("reasoningOptions");
                if(rs!=null)for(int k=0;k<rs.length();k++){String r2=rs.optString(k);if(!r2.isEmpty())ro.add(r2);}
                out.add(new Provider(o.optString("id"),o.optString("displayName"),o.optString("baseUrl"),o.optString("protocol","OPENAI"),o.optString("apiKey"),models,ro));}
        }catch(Exception e){
            // F3: corruption must not silently become an empty table that a later save overwrites.
            String raw=prefs(c).getString(KEY_LIST,null);
            if(raw!=null)prefs(c).edit().putString(KEY_LIST+"_corrupt_backup",raw).apply();
        }
        return out;
    }
    public static synchronized void save(Context c,List<Provider> providers){
        try{JSONArray a=new JSONArray();
            for(Provider p:providers){JSONObject o=new JSONObject();
                o.put("id",p.id).put("displayName",p.displayName).put("baseUrl",p.baseUrl).put("protocol",p.protocol).put("apiKey",p.apiKey);
                JSONArray ms=new JSONArray();for(String m:p.models)ms.put(m);o.put("models",ms);
                JSONArray ro=new JSONArray();for(String r2:p.reasoningOptions)ro.put(r2);o.put("reasoningOptions",ro);
                a.put(o);}
            prefs(c).edit().putString(KEY_LIST,a.toString()).apply();
        }catch(Exception ignored){}
    }
    public static synchronized void upsert(Context c,Provider p){
        List<Provider> all=list(c);
        for(int i=0;i<all.size();i++) if(all.get(i).id.equals(p.id)){all.set(i,p);save(c,all);return;}
        all.add(p);save(c,all);
    }
    public static synchronized void remove(Context c,String id){
        List<Provider> all=list(c);
        for(int i=0;i<all.size();i++) if(all.get(i).id.equals(id)){all.remove(i);save(c,all);break;}
    }
    public static synchronized void removeModel(Context c,String providerId,String model){
        List<Provider> all=list(c);
        for(Provider p:all)if(p.id.equals(providerId)){p.models.remove(model);break;}
        save(c,all);
    }
    public static Provider find(Context c,String id){for(Provider p:list(c))if(p.id.equals(id))return p;return null;}
    /** Active model is stored as "providerId|model". */
    public static String activeKey(Context c){return prefs(c).getString(KEY_ACTIVE,"");}
    public static void setActive(Context c,String providerId,String model){prefs(c).edit().putString(KEY_ACTIVE,providerId+"|"+model).apply();}
    public static Provider activeProvider(Context c){
        String key=activeKey(c);int idx=key.indexOf('|');
        if(idx>0){Provider p=find(c,key.substring(0,idx));if(p!=null&&p.models.contains(key.substring(idx+1)))return p;}
        List<Provider> all=list(c);
        for(Provider p:all)if(!p.models.isEmpty()){setActive(c,p.id,p.models.get(0));return p;}
        return null;
    }
    public static String activeModel(Context c){
        String key=activeKey(c);int idx=key.indexOf('|');
        if(idx>0){String m=key.substring(idx+1);Provider p=find(c,key.substring(0,idx));if(p!=null&&p.models.contains(m))return m;}
        Provider p=activeProvider(c);return p==null||p.models.isEmpty()?"":p.models.get(0);
    }
    /** F5: legacy slots may hold arbitrary strings; never let valueOf crash on them. */
    public static String normalizeProtocol(String raw){
        if(raw==null)return "OPENAI";
        String v=raw.trim().toUpperCase(Locale.ROOT);
        return v.equals("ANTHROPIC")?"ANTHROPIC":"OPENAI";
    }
    /** Legacy migration: one provider per non-empty old preset slot, its model pre-added. */
    public static void migrateLegacy(Context c){
        if(prefs(c).getBoolean("migrated",false))return;
        SharedPreferences ui=c.getSharedPreferences("chat_settings_ui",Context.MODE_PRIVATE);
        List<Provider> migrated=new ArrayList<>();
        for(int i=0;i<5;i++){
            String url=ui.getString("preset_"+i+"_url","").trim();
            String key=ui.getString("preset_"+i+"_key","").trim();
            String model=ui.getString("preset_"+i+"_model","").trim();
            if(url.isEmpty()&&key.isEmpty()&&model.isEmpty())continue;
            String protocol=normalizeProtocol(ui.getString("preset_"+i+"_provider","OPENAI"));
            String name=ui.getString("presetName_"+i,"").trim();
            List<String> models=new ArrayList<>();if(!model.isEmpty())models.add(model);
            migrated.add(new Provider("legacy_"+i,name.isEmpty()?"预设"+(i+1):name,url,protocol,key,models));
        }
        if(!migrated.isEmpty()&&list(c).isEmpty())save(c,migrated);

        prefs(c).edit().putBoolean("migrated",true).apply();
    }
}
