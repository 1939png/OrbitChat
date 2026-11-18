package com.example.watchchat.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Output destination preference and private-directory writer. */
public final class FileFeatureStore {
    private static final String PREFS="file_features"; private static final String SYSTEM="system_directory";
    private FileFeatureStore(){}
    public static boolean useSystemDirectory(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(SYSTEM,false);}
    public static void setUseSystemDirectory(Context c,boolean value){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(SYSTEM,value).apply();}
    public static File privateOutput(Context c,String name)throws Exception{File dir=new File(c.getFilesDir(),"exports");if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("无法创建输出目录");return new File(dir,safeName(name));}
    public static void writePrivate(Context c,String name,String content)throws Exception{File dir=new File(c.getFilesDir(),"exports");if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("无法创建输出目录");try(FileOutputStream out=new FileOutputStream(new File(dir,safeName(name)))){out.write(content.getBytes(StandardCharsets.UTF_8));}}
    public static String safeName(String name){String n=name==null?"answer.txt":name.replaceAll("[^a-zA-Z0-9._-]","_");return n.isEmpty()?"answer.txt":n;}
}
