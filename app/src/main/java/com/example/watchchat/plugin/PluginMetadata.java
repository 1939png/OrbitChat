package com.example.watchchat.plugin;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Immutable plugin metadata. 不可变插件元数据。 */
public final class PluginMetadata {
 private final String id,name,version,description; private final Map<String,String> attributes;
 public PluginMetadata(String id,String name,String version,String description){this(id,name,version,description,null);}
 public PluginMetadata(String id,String name,String version,String description,Map<String,String> attributes){this.id=req(id,"id");this.name=req(name,"name");this.version=req(version,"version");this.description=description==null?"":description;Map<String,String> copy=new LinkedHashMap<>();if(attributes!=null)copy.putAll(attributes);this.attributes=Collections.unmodifiableMap(copy);}
 public String getId(){return id;} public String getName(){return name;} public String getVersion(){return version;} public String getDescription(){return description;} public Map<String,String> getAttributes(){return attributes;}
 private static String req(String v,String n){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(n+" is required");return v;}
}
