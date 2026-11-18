package com.example.watchchat;

/** Standalone regression checks: javac -encoding UTF-8 -d OUT Core.java Test.java; java -cp OUT com.example.watchchat.MessageFormatCoreTest */
public final class MessageFormatCoreTest {
    private static int checks;
    private static void equal(String input,String expected) {
        MessageFormatCore.Result result=MessageFormatCore.parse(input);
        if(!expected.equals(result.text.toString()))throw new AssertionError("input="+input+" expected="+expected+" actual="+result.text);
        for(MessageFormatCore.Mark mark:result.marks)if(mark.start<0 || mark.end>result.text.length() || mark.start>=mark.end)throw new AssertionError("invalid span");
        checks++;
    }
    public static void main(String[] args) {
        equal(null,"");equal("","");
        equal("\\[\n2H_2 + O_2 \\xrightarrow{\\text{点燃}} 2H_2O\n\\]","2H₂ + O₂  →（点燃）  2H₂O\n");
        equal("\\[\n2\\mathrm{KMnO_4}+16\\mathrm{HCl}\\rightarrow 2\\mathrm{KCl}+2\\mathrm{MnCl_2}+5\\mathrm{Cl_2}\\uparrow+8\\mathrm{H_2O}\n\\]","2KMnO₄+16HCl→ 2KCl+2MnCl₂+5Cl₂↑+8H₂O\n");
        equal("\\[\n2\\mathrm{MnO_4^-}+16\\mathrm{H^+}+10\\mathrm{Cl^-}\n\\rightarrow 2\\mathrm{Mn^{2+}}+5\\mathrm{Cl_2}\\uparrow+8\\mathrm{H_2O}\n\\]","2MnO₄⁻+16H⁺+10Cl⁻\n→ 2Mn²⁺+5Cl₂↑+8H₂O\n");
        String plain="中文，。；;！？—…🙂👩🏽‍💻 café é\r\n下一行\n";equal(plain,plain);
        equal("# 标题🙂\n**粗体；** 普通", "标题🙂\n粗体； 普通");
        MessageFormatCore.Result spans=MessageFormatCore.parse("# 标题\n**bold**");
        if(spans.marks.size()!=3)throw new AssertionError("heading/bold spans missing");checks++;
        equal("$E=mc^2$", "E=mc²");
        equal("\\(x_1 + \\alpha \\leq 2\\)","x₁ + α ≤ 2");
        equal("$\\frac{1}{\\sqrt{x^2+1}}$","(1)/(√(x²+1))");
        equal("$$\n\\sum_{i=1}^{n} i^2\n$$\n", "∑ᵢ₌₁ⁿ i²\n");
        equal("\\[\nx^2\n\\]\n", "x²\n");
        String unknown="$\\unknown{x} + \\alpha$";equal(unknown,unknown);
        String matrix="$$\n\\begin{matrix}a&b\\end{matrix}\n$$";equal(matrix,matrix);
        String broken="$\\frac{1}{2$";equal(broken,broken);
        equal("$x^{abc}$", "$x^{abc}$");
        equal("$x^q$", "$x^q$");
        equal("价格 $5 和 $10；;", "价格 $5 和 $10；;");
        equal("**未闭合🙂", "**未闭合🙂");equal("$x^2", "$x^2");
        String tick=String.valueOf((char)96);
        equal(tick+"$x^2$; **literal**"+tick,"$x^2$; **literal**");
        String fence=tick+tick+tick;
        equal(fence+"java\nString x = \"$x^2$\";\n"+fence+"\n", "java\nString x = \"$x^2$\";\n");
        String incomplete=fence+"\n**keep** $x^2$\n";equal(incomplete,incomplete);
        equal("| 名称 | 值 |\n| --- | :---: |\n| α； | $x^2$ |\n", "名称 │ 值\n────────\n名称: α；\n值: x²\n\n");
        equal("| A | B |\n| --- | --- |\n| a\\|b | "+tick+"x|y"+tick+" | extra |", "A │ B\n────────\nA: a|b\nB: x|y\n[3]: extra\n\n");
        equal("a | b\n-- | --\n", "a | b\n-- | --\n");
        String deep="$\\frac{";for(int i=0;i<40;i++)deep+="{";deep+="x";for(int i=0;i<40;i++)deep+="}";deep+="}{1}$";equal(deep,deep);
        String[] samples={plain, unknown, matrix, incomplete, "# Heading\n**bold** and $x^2$", "|a|b|\n|---|---|\n|🙂|;|"};
        for(String sample:samples)for(int i=0;i<=sample.length();i++) {
            MessageFormatCore.Result result=MessageFormatCore.parse(sample.substring(0,i));
            for(MessageFormatCore.Mark mark:result.marks)if(mark.start<0 || mark.end>result.text.length())throw new AssertionError("prefix span");
            checks++;
        }
        System.out.println("PASS "+checks+" formatter checks");
    }
}
