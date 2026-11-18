package com.example.watchchat;
import java.util.List;

/** Runs on the JVM with android.jar for class resolution; no Android views instantiated. */
public final class AssistantMessageViewTest {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
    public static void main(String[] args){
        String table="| 名称 | 值 |\n| --- | :---: |\n| a\\|b | `x|y` |\n";
        List<AssistantMessageView.Block> blocks=AssistantMessageView.blocks(table);
        check(blocks.size()==1&&blocks.get(0).table(),"native table block");
        check(blocks.get(0).rows.get(1).get(0).equals("a|b"),"escaped pipe");
        check(blocks.get(0).rows.get(1).get(1).equals("`x|y`"),"inline code pipe");
        for(String fence:new String[]{"```","~~~~"}){
            String code=fence+"java\n"+table+fence+"\n";
            blocks=AssistantMessageView.blocks(code+table);
            check(blocks.size()==2&&!blocks.get(0).table()&&blocks.get(1).table(),"fence isolation");
            check(blocks.get(0).text.equals(code),"code source preserved");
            blocks=AssistantMessageView.blocks(fence+"\n"+table);
            check(blocks.size()==1&&!blocks.get(0).table(),"unclosed fence isolation");
        }
        String raw="中文；;🙂👩🏽‍💻 café é &amp; &#59; \uFFFD\n";
        blocks=AssistantMessageView.blocks(raw);
        check(blocks.get(0).text.equals(raw),"unicode and entities preserved");
        check(MessageFormatCore.parse(raw).text.toString().equals(raw),"formatter preserves source characters");
        String math="$$\n\\begin{matrix}a&b\\end{matrix}\n$$";
        check(MessageFormatCore.parse(math).text.toString().equals(math),"unsupported math preserved");
        check(!AssistantMessageView.blocks("a|b\n--|--").get(0).table(),"invalid separator");
        check(AssistantMessageView.blocks("before\n"+table+"\nafter").size()==3,"mixed blocks");
        for(int i=0;i<=table.length();i++){
            blocks=AssistantMessageView.blocks(table.substring(0,i));
            for(AssistantMessageView.Block block:blocks)check(block.table()? !block.rows.isEmpty():block.text!=null,"stream prefix");
        }
        System.out.println("PASS "+checks+" message block checks");
    }
}
