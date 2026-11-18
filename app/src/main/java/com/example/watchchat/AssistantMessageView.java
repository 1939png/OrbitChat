package com.example.watchchat;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Display only: raw is never rewritten or used as formatted storage. */
final class AssistantMessageView extends LinearLayout {
    private final int textSize, color;
    private String raw="";
    private final List<Block> rendered=new ArrayList<>();
    private boolean queued;
    private Runnable afterRender;
    private final Runnable renderPending=()->{queued=false; renderRaw(); if(afterRender!=null)afterRender.run();};
    AssistantMessageView(Context context,int textSize,int color) {
        super(context); this.textSize=textSize; this.color=color;
        setOrientation(VERTICAL);
        // Deliberately no long-press handler anywhere in this view: the message block spans
        // most of the screen, so any long-press listener here fires during a horizontal
        // pager swipe and pops a dialog the user never asked for.
    }
    String getRaw() { return raw; }
    void setRaw(String value) { raw=value==null?"":value; removeCallbacks(renderPending);queued=false;renderRaw(); }
    void streamRaw(String value,Runnable after) {
        raw=value==null?"":value;afterRender=after;
        if(!queued){queued=true;postDelayed(renderPending,80);}
    }
    @Override protected void onDetachedFromWindow(){removeCallbacks(renderPending);queued=false;super.onDetachedFromWindow();}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView textView(){
        TextView text=new TextView(getContext());text.setTextColor(color);text.setTextSize(textSize);
        text.setGravity(Gravity.TOP);text.setLineSpacing(dp(2),1.05f);
        text.setFocusable(false);text.setLongClickable(false);text.setClickable(false);
        // No long-press handler: a long press during a horizontal pager swipe was
        // opening this dialog, which read as a stray system menu.
        return text;
    }
    private void showRaw(){
        final String original=raw;
        TextView text=textView();text.setText(original);text.setTextIsSelectable(false);text.setPadding(dp(12),dp(8),dp(12),dp(8));
        ScrollView scroll=new ScrollView(getContext());scroll.addView(text);
        new AlertDialog.Builder(getContext()).setTitle("消息原文").setView(scroll)
            .setPositiveButton("复制原文",(d,w)->((ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("消息原文",original)))
            .setNegativeButton("关闭",null).show();
    }
    static final class Block {
        final String text; final List<List<String>> rows;
        Block(String text,List<List<String>> rows){this.text=text;this.rows=rows;}
        boolean table(){return rows!=null;}
    }
    static List<Block> blocks(String raw){
        List<Block> out=new ArrayList<>();StringBuilder plain=new StringBuilder();
        String[] lines=raw.split("(?<=\n)",-1);String fence=null;
        for(int i=0;i<lines.length;i++){
            String trim=lines[i].trim();
            if(fence!=null){plain.append(lines[i]);if(trim.matches(java.util.regex.Pattern.quote(fence.charAt(0)+"")+"{"+fence.length()+",}"))fence=null;continue;}
            if(trim.startsWith("~~~")||trim.startsWith("```")){
                int n=0;while(n<trim.length()&&trim.charAt(n)==trim.charAt(0))n++;
                fence=trim.substring(0,n);plain.append(lines[i]);continue;
            }
            // Indented code and display mathematics remain opaque source blocks.
            if(lines[i].startsWith("    ")||lines[i].startsWith("\t")){plain.append(lines[i]);continue;}
            if(trim.equals("$$")||trim.equals("\\[")){
                String close=trim.equals("$$")?"$$":"\\]";plain.append(lines[i]);
                while(++i<lines.length){plain.append(lines[i]);if(lines[i].trim().equals(close))break;}continue;
            }
            List<String> header=MessageFormatCore.cells(lines[i]);
            if(header!=null&&i+1<lines.length&&MessageFormatCore.separator(lines[i+1].trim(),header.size())){
                if(plain.length()>0){out.add(new Block(plain.toString(),null));plain.setLength(0);}
                List<List<String>> rows=new ArrayList<>();rows.add(header);i+=2;
                for(;i<lines.length;i++){
                    String next=lines[i].trim();
                    if(next.startsWith("~~~")||next.startsWith("```"))break;
                    List<String> cells=MessageFormatCore.cells(lines[i]);if(cells==null)break;rows.add(cells);
                }
                i--;out.add(new Block(null,rows));
            }else plain.append(lines[i]);
        }
        if(plain.length()>0)out.add(new Block(plain.toString(),null));return out;
    }
    private void renderRaw(){
        List<Block> next=blocks(raw);
        for(int i=0;i<next.size();i++){
            Block block=next.get(i);Block previous=i<rendered.size()?rendered.get(i):null;
            if(previous==null||previous.table()!=block.table()){
                if(i<getChildCount())removeViewAt(i);
                View child=block.table()?new Grid(getContext()):textView();
                addView(child,i,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
            }
            if(block.table())((Grid)getChildAt(i)).update(block.rows);
            else if(previous==null||!block.text.equals(previous.text))((TextView)getChildAt(i)).setText(MessageFormatter.format(block.text));
        }
        while(getChildCount()>next.size())removeViewAt(getChildCount()-1);
        rendered.clear();rendered.addAll(next);
    }
    final class Grid extends HorizontalScrollView {
        final TableLayout table;
        final List<List<String>> cached=new ArrayList<>();
        float startX,startY;
        boolean horizontal;
        Grid(Context context){
            super(context);setHorizontalScrollBarEnabled(true);setFillViewport(false);
            table=new TableLayout(context);addView(table,new ViewGroup.LayoutParams(-2,-2));
            setContentDescription("表格，可左右滑动；长按单元格查看原文");
        }
        @Override public boolean dispatchTouchEvent(MotionEvent e){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){startX=e.getX();startY=e.getY();horizontal=false;}
            else if(action==MotionEvent.ACTION_MOVE){
                float dx=Math.abs(e.getX()-startX),dy=Math.abs(e.getY()-startY);
                if(!horizontal&&dx>android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop()&&dx>dy)horizontal=true;
                if(horizontal)getParent().requestDisallowInterceptTouchEvent(true);
            }
            boolean handled=super.dispatchTouchEvent(e);
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){horizontal=false;getParent().requestDisallowInterceptTouchEvent(false);}
            return handled;
        }
        void update(List<List<String>> rows){
            int columns=0;for(List<String> row:rows)columns=Math.max(columns,row.size());
            for(int r=0;r<rows.size();r++){
                TableRow row;
                if(r<table.getChildCount())row=(TableRow)table.getChildAt(r);
                else {row=new TableRow(getContext());table.addView(row,new TableLayout.LayoutParams(-2,-2));}
                for(int c=0;c<columns;c++){
                    TextView cell;
                    boolean fresh=c>=row.getChildCount();
                    if(fresh){
                        cell=textView();cell.setPadding(dp(9),dp(7),dp(9),dp(7));
                        GradientDrawable background=new GradientDrawable();background.setColor(r==0?0xFF252B21:0xFF111410);background.setStroke(dp(1),0xFF61695B);cell.setBackground(background);
                        if(r==0)cell.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                        row.addView(cell,new TableRow.LayoutParams(dp(160),-1));
                    }else cell=(TextView)row.getChildAt(c);
                    String value=c<rows.get(r).size()?rows.get(r).get(c):"";
                    if(fresh||r>=cached.size()||c>=cached.get(r).size()||!value.equals(cached.get(r).get(c)))cell.setText(MessageFormatter.format(value));
                }
                while(row.getChildCount()>columns)row.removeViewAt(row.getChildCount()-1);
            }
            while(table.getChildCount()>rows.size())table.removeViewAt(table.getChildCount()-1);
            cached.clear();cached.addAll(rows);
        }
    }
    static boolean touchesTable(View view,float x,float y){
        if(view==null||view.getVisibility()!=VISIBLE)return false;
        int[] pos=new int[2];view.getLocationOnScreen(pos);
        if(x<pos[0]||y<pos[1]||x>=pos[0]+view.getWidth()||y>=pos[1]+view.getHeight())return false;
        if(view instanceof Grid)return true;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(touchesTable(group.getChildAt(i),x,y))return true;}
        return false;
    }
}
