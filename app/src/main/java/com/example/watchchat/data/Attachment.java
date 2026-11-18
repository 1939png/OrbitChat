package com.example.watchchat.data;

/** Auditable attachment; images carry in-memory base64 for multimodal requests. */
public final class Attachment {
    public enum Kind { IMAGE, TEXT }
    public final Kind kind; public final String displayName; public final String mimeType; public final String uri; public final String text; public final String base64;
    Attachment(Kind kind,String displayName,String mimeType,String uri,String text,String base64){this.kind=kind;this.displayName=displayName;this.mimeType=mimeType;this.uri=uri;this.text=text==null?"":text;this.base64=base64==null?"":base64;}
    public static Attachment image(String name,String mime,String uri){return new Attachment(Kind.IMAGE,name,mime,uri,"",null);}
    public static Attachment imageData(String name,String mime,String uri,String base64){return new Attachment(Kind.IMAGE,name,mime,uri,"",base64);}
    public static Attachment text(String name,String mime,String uri,String value){return new Attachment(Kind.TEXT,name,mime,uri,value,"");}
    public String auditLabel(){return "[附件 " + (kind==Kind.IMAGE?"图片":"文本") + "：" + displayName + "]";}
    public String toPromptText(){if(kind==Kind.TEXT)return auditLabel()+"\n<attachment_text>\n"+text+"\n</attachment_text>";return auditLabel()+"\nURI: "+uri+"\nMIME: "+mimeType+"\n（图片内容已随多模态请求发送；历史记录仅保留此说明。）";}
    public boolean hasImageData(){return kind==Kind.IMAGE&&!base64.isEmpty();}
}
