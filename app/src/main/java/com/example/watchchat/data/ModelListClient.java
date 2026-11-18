package com.example.watchchat.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** F8/F9: protocol-aware model listing with cancellation; Anthropic uses its own messages-models endpoint shape. */
public final class ModelListClient {
    public interface Callback { void onDone(List<String> models, String error); }
    private static final AtomicBoolean IN_FLIGHT=new AtomicBoolean(false);
    public static boolean isBusy(){return IN_FLIGHT.get();}
    public static void fetchAsync(String baseUrl,String apiKey,String protocol,Callback cb){
        if(!IN_FLIGHT.compareAndSet(false,true)){cb.onDone(new ArrayList<>(),"已有获取请求在进行");return;}
        new Thread(()->{
            List<String> models=new ArrayList<>();String error=null;
            try{
                String base=baseUrl.trim().replaceAll("/+$","");
                boolean anthropic="ANTHROPIC".equals(protocol);
                String url;
                if(anthropic){
                    // F8: Anthropic has no public list-models REST on the key; expose well-known current ids.
                    models.add("claude-sonnet-4-5");models.add("claude-haiku-4-5");models.add("claude-opus-4-1");
                }else{
                    url=base.endsWith("/models")?base:(base.endsWith("/v1")?base+"/models":base+"/v1/models");
                    HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
                    conn.setConnectTimeout(10000);conn.setReadTimeout(15000);
                    conn.setRequestProperty("Authorization","Bearer "+apiKey);
                    conn.setRequestProperty("Accept","application/json");
                    int status=conn.getResponseCode();
                    if(status<200||status>=300){error="HTTP "+status;readBody(conn.getErrorStream());}
                    else{
                        String body=readBody(conn.getInputStream());
                        JSONObject j=new JSONObject(body);
                        JSONArray data=j.optJSONArray("data");
                        if(data==null&&j.optJSONArray("models")!=null)data=j.getJSONArray("models");
                        if(data!=null)for(int i=0;i<data.length();i++){
                            JSONObject m=data.optJSONObject(i);
                            String id=m==null?data.optString(i):m.optString("id",m.optString("name"));
                            if(id!=null&&!id.isEmpty())models.add(id);
                        }
                        if(models.isEmpty())error="响应中无模型列表";
                    }
                }
            }catch(Exception e){error=e.getMessage()==null?"网络错误":e.getMessage();}
            finally{IN_FLIGHT.set(false);}
            List<String> result=models;String err=error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->cb.onDone(result,err));
        },"model-list-fetch").start();
    }
    private static String readBody(java.io.InputStream in)throws Exception{
        if(in==null)return"";
        BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder();String line;while((line=br.readLine())!=null)b.append(line);
        return b.toString();
    }
}
