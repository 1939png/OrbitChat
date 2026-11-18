package com.example.watchchat;

import com.example.watchchat.data.Attachment;

/** Standalone checks: compile with Attachment.java, then run this class. */
public final class AttachmentTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        Attachment image=Attachment.image("photo.jpg","image/jpeg","content://photo");
        check(image.toPromptText().contains("兼容文本降级")==false||image.toPromptText().contains("图片"),"image audit");
        check(!image.hasImageData(),"metadata-only image");
        Attachment loaded=Attachment.imageData("photo.jpg","image/jpeg","content://photo","aGVsbG8=");
        check(loaded.hasImageData(),"base64 present");
        check(loaded.toPromptText().contains("多模态"),"multimodal note");
        System.out.println("PASS attachment checks");
    }
}
