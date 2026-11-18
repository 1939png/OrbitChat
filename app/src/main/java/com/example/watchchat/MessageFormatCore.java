package com.example.watchchat;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Small display-only subset; unsupported formulas fall back atomically to their source. */
final class MessageFormatCore {
    static final int BOLD=1, CODE=2, HEADING=3;
    static final class Mark {
        final int start,end,kind;
        Mark(int s,int e,int k) { start=s; end=e; kind=k; }
    }
    static final class Result {
        final StringBuilder text=new StringBuilder();
        final List<Mark> marks=new ArrayList<>();
        void mark(int start,int kind) { if(text.length()>start) marks.add(new Mark(start,text.length(),kind)); }
    }
    private MessageFormatCore() {}
    static Result parse(String raw) {
        Result out=new Result();
        if(raw==null || raw.isEmpty()) return out;
        String[] lines=raw.split("(?<=\\n)",-1);
        for(int i=0;i<lines.length;i++) {
            String line=body(lines[i]);
            String trim=line.trim();
            if(trim.startsWith("~~~") || (trim.length()>=3 && trim.charAt(0)==96 && trim.charAt(1)==96 && trim.charAt(2)==96)) {
                int n=0; while(n<trim.length() && trim.charAt(n)==trim.charAt(0)) n++;
                String fence=trim.substring(0,n); int end=i+1;
                while(end<lines.length && !body(lines[end]).trim().equals(fence)) end++;
                if(end==lines.length) { for(;i<lines.length;i++) out.text.append(lines[i]); break; }
                int start=out.text.length();
                String language=trim.substring(n);
                if(!language.isEmpty()) out.text.append(language).append(eol(lines[i]));
                for(int j=i+1;j<end;j++) out.text.append(lines[j]);
                out.mark(start,CODE);
                if(out.text.length()==start || out.text.charAt(out.text.length()-1)!='\n') out.text.append(eol(lines[end]));
                i=end; continue;
            }
            if(trim.equals("$$") || trim.equals("\\[")) {
                String close=trim.equals("$$")?"$$":"\\]"; int end=i+1;
                while(end<lines.length && !body(lines[end]).trim().equals(close))end++;
                if(end<lines.length) {
                    StringBuilder expression=new StringBuilder();
                    for(int j=i+1;j<end;j++)expression.append(lines[j]);
                    String converted=math(expression.toString());
                    if(converted==null)for(int j=i;j<=end;j++)out.text.append(lines[j]);
                    else {out.text.append(converted); if(!converted.endsWith("\n"))out.text.append(eol(lines[end]));}
                    i=end;continue;
                }
            }
            List<String> header=cells(line);
            if(header!=null && i+1<lines.length && separator(body(lines[i+1]),header.size())) {
                for(int c=0;c<header.size();c++) { if(c>0)out.text.append(" │ "); int start=out.text.length(); inline(header.get(c),out,0); out.mark(start,BOLD); }
                out.text.append("\n────────\n"); i+=2;
                while(i<lines.length) {
                    List<String> row=cells(body(lines[i])); if(row==null)break;
                    for(int c=0;c<row.size();c++) {
                        if(c<header.size()) inline(header.get(c),out,0); else out.text.append('[').append(c+1).append(']');
                        out.text.append(": "); inline(row.get(c),out,0); out.text.append('\n');
                    }
                    out.text.append('\n'); i++;
                }
                i--; continue;
            }
            int hashes=0; while(hashes<line.length() && line.charAt(hashes)=='#')hashes++;
            if(hashes>0 && hashes<=6 && hashes<line.length() && line.charAt(hashes)==' ') {
                int start=out.text.length(); inline(line.substring(hashes+1),out,0); out.mark(start,BOLD); out.mark(start,HEADING);
            } else inline(line,out,0);
            out.text.append(eol(lines[i]));
        }
        return out;
    }
    private static String body(String s) { return s.endsWith("\r\n")?s.substring(0,s.length()-2):s.endsWith("\n")?s.substring(0,s.length()-1):s; }
    private static String eol(String s) { return s.endsWith("\r\n")?"\r\n":s.endsWith("\n")?"\n":""; }
    static List<String> cells(String s) {
        List<String> result=new ArrayList<>(); StringBuilder cell=new StringBuilder(); int ticks=0; boolean pipe=false;
        for(int i=0;i<s.length();i++) {
            char ch=s.charAt(i);
            if(ch=='\\' && i+1<s.length() && s.charAt(i+1)=='|') { cell.append('|'); i++; continue; }
            if(ch==96) {
                int n=1; while(i+n<s.length() && s.charAt(i+n)==96)n++;
                if(ticks==0)ticks=n; else if(ticks==n)ticks=0;
                cell.append(s,i,i+n); i+=n-1; continue;
            }
            if(ch=='|' && ticks==0) { result.add(cell.toString().trim());cell.setLength(0);pipe=true; } else cell.append(ch);
        }
        if(!pipe)return null;
        result.add(cell.toString().trim());
        if(s.trim().startsWith("|"))result.remove(0);
        if(s.trim().endsWith("|") && !s.trim().endsWith("\\|") && !result.isEmpty())result.remove(result.size()-1);
        return result.isEmpty()?null:result;
    }
    static boolean separator(String s,int count) {
        List<String> row=cells(s);if(row==null || row.size()!=count)return false;
        for(String cell:row)if(!cell.matches(":?-{3,}:?"))return false;
        return true;
    }
    private static void inline(String s,Result out,int depth) {
        if(depth>16) {out.text.append(s);return;}
        for(int i=0;i<s.length();) {
            char ch=s.charAt(i);
            if(ch==96) {
                int n=1;while(i+n<s.length() && s.charAt(i+n)==96)n++;
                String token=s.substring(i,i+n);int end=s.indexOf(token,i+n);
                if(end>=0) {int start=out.text.length();out.text.append(s,i+n,end);out.mark(start,CODE);i=end+n;continue;}
                out.text.append(s.substring(i));break;
            }
            String open=s.startsWith("\\(",i)?"\\(":s.startsWith("\\[",i)?"\\[":s.startsWith("$$",i)?"$$":ch=='$'?"$":null;
            if(open!=null) {
                String close=open.equals("\\(")?"\\)":open.equals("\\[")?"\\]":open;
                int end=s.indexOf(close,i+open.length());
                if(end>=0) {
                    String converted=math(s.substring(i+open.length(),end));
                    if(converted==null)out.text.append(s,i,end+close.length());else out.text.append(converted);
                    i=end+close.length();continue;
                }
                out.text.append(s.substring(i));break;
            }
            if(s.startsWith("**",i) || s.startsWith("__",i)) {
                String token=s.substring(i,i+2);int end=s.indexOf(token,i+2);
                if(end>i+2) {int start=out.text.length();inline(s.substring(i+2,end),out,depth+1);out.mark(start,BOLD);i=end+2;continue;}
            }
            if(ch=='\\' && i+1<s.length() && ("*$#_".indexOf(s.charAt(i+1))>=0 || s.charAt(i+1)==96)) {out.text.append(s,i,i+2);i+=2;continue;}
            out.text.append(ch);i++;
        }
    }
    private static final Map<String,String> SYMBOLS=new HashMap<>();
    static {
        String[] names={"alpha","beta","gamma","delta","epsilon","theta","lambda","mu","pi","rho","sigma","tau","phi","omega","Gamma","Delta","Theta","Lambda","Pi","Sigma","Phi","Omega","times","cdot","div","pm","mp","le","leq","ge","geq","ne","neq","approx","equiv","infty","sum","prod","int","partial","nabla","in","notin","subset","subseteq","cup","cap","to","rightarrow","leftarrow","Rightarrow","Leftrightarrow","forall","exists","degree","ldots","cdots"};
        String[] values={"α","β","γ","δ","ε","θ","λ","μ","π","ρ","σ","τ","φ","ω","Γ","Δ","Θ","Λ","Π","Σ","Φ","Ω","×","·","÷","±","∓","≤","≤","≥","≥","≠","≠","≈","≡","∞","∑","∏","∫","∂","∇","∈","∉","⊂","⊆","∪","∩","→","→","←","⇒","⇔","∀","∃","°","…","⋯"};
        for(int i=0;i<names.length;i++)SYMBOLS.put(names[i],values[i]);
        SYMBOLS.put("uparrow","↑");SYMBOLS.put("downarrow","↓");SYMBOLS.put("rightleftharpoons","⇌");SYMBOLS.put("longrightarrow","⟶");
    }
    private static String math(String s) {
        if(s.trim().isEmpty() || (!s.contains("\\") && !s.contains("^") && !s.contains("_") && !s.contains("=")))return null;
        try {MathReader reader=new MathReader(s);String result=reader.read(false,0);return reader.pos==s.length()?result:null;}
        catch(IllegalArgumentException unsupported){return null;}
    }
    private static final class MathReader {
        final String source;int pos;
        MathReader(String source){this.source=source;}
        String read(boolean grouped,int depth) {
            if(depth>32)throw new IllegalArgumentException();
            StringBuilder out=new StringBuilder();
            while(pos<source.length()) {
                char ch=source.charAt(pos++);
                if(ch=='}'){if(grouped)return out.toString();throw new IllegalArgumentException();}
                if(ch=='{'){out.append('{').append(read(true,depth+1)).append('}');continue;}
                if(ch=='^' || ch=='_') {
                    String arg=argument(depth+1),mapped=script(arg,ch=='^');
                    if(mapped==null)throw new IllegalArgumentException();
                    out.append(mapped);
                    continue;
                }
                if(ch=='\\') {
                    int start=pos;while(pos<source.length() && Character.isLetter(source.charAt(pos)))pos++;
                    String name=source.substring(start,pos);
                    if(SYMBOLS.containsKey(name))out.append(SYMBOLS.get(name));
                    else if(name.equals("frac") || name.equals("dfrac") || name.equals("tfrac")) {String a=group(depth+1),b=group(depth+1);out.append('(').append(a).append(")/(").append(b).append(')');}
                    else if(name.equals("xrightarrow") || name.equals("xleftarrow")) {String condition=group(depth+1);out.append(name.equals("xrightarrow")?" →":" ←").append("（").append(condition).append("） ");}
                    else if(name.equals("sqrt"))out.append("√(").append(group(depth+1)).append(')');
                    else if(name.equals("text") || name.equals("mathrm") || name.equals("mathbf"))out.append(group(depth+1));
                    else if(name.equals("sin") || name.equals("cos") || name.equals("tan") || name.equals("log") || name.equals("ln") || name.equals("lim"))out.append(name);
                    else if(name.equals("left") || name.equals("right")) {if(pos>=source.length() || "()[]|".indexOf(source.charAt(pos))<0)throw new IllegalArgumentException();out.append(source.charAt(pos++));}
                    else if(name.isEmpty() && pos<source.length() && ",;! ".indexOf(source.charAt(pos))>=0){pos++;out.append(' ');}
                    else if(name.isEmpty() && pos<source.length() && "{}_%$#".indexOf(source.charAt(pos))>=0)out.append(source.charAt(pos++));
                    else throw new IllegalArgumentException();
                    continue;
                }
                if(ch=='&' || ch=='$')throw new IllegalArgumentException();
                out.append(ch);
            }
            if(grouped)throw new IllegalArgumentException();return out.toString();
        }
        String group(int depth) {
            while(pos<source.length() && Character.isWhitespace(source.charAt(pos)))pos++;
            if(pos>=source.length() || source.charAt(pos++)!='{')throw new IllegalArgumentException();
            return read(true,depth);
        }
        String argument(int depth) {
            if(pos>=source.length())throw new IllegalArgumentException();
            if(source.charAt(pos)=='{'){pos++;return read(true,depth);}
            char ch=source.charAt(pos++);
            if(ch=='\\' || ch=='}' || ch=='^' || ch=='_' || Character.isWhitespace(ch))throw new IllegalArgumentException();
            return String.valueOf(ch);
        }
    }
    private static String script(String s,boolean upper) {
        String keys=upper?"0123456789+-=()ni":"0123456789+-=()aehijklmnoprstuvx";
        String values=upper?"⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁿⁱ":"₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎ₐₑₕᵢⱼₖₗₘₙₒₚᵣₛₜᵤᵥₓ";
        StringBuilder out=new StringBuilder();if(s.isEmpty())return null;
        for(int i=0;i<s.length();i++){int at=keys.indexOf(s.charAt(i));if(at<0)return null;out.append(values.charAt(at));}
        return out.toString();
    }
}
