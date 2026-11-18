package com.example.watchchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** SAF directory file browser: browse/read/rename/delete inside a user-granted tree. */
public final class FileBrowser extends Dialog {
    public interface Listener {
        File treeRootDir();
        void onPickTextFile(File f);
        void onTreeGranted();
        void requestTreeGrant();
        void onUseAppDir();
    }
    private final Listener listener;
    private File current;
    private LinearLayout listBody;
    private TextView pathLabel;
    private final int size,fg,secondary,accent;
    public FileBrowser(Activity a,Listener listener,int size,int fg,int secondary,int accent){
        super(a);this.listener=listener;this.size=size;this.fg=fg;this.secondary=secondary;this.accent=accent;
        setOwnerActivity(a);setCancelable(true);setCanceledOnTouchOutside(true);
    }
    @Override protected void onCreate(Bundle state){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=new LinearLayout(getContext());panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(14),dp(12),dp(14),dp(14));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xff15171a);bg.setCornerRadius(dp(30));panel.setBackground(bg);
        TextView title=text("文件管理",20,fg);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);title.setPadding(0,0,0,dp(8));panel.addView(title);
        pathLabel=text("",12,secondary);pathLabel.setPadding(dp(4),0,dp(4),dp(6));panel.addView(pathLabel);
        listBody=new LinearLayout(getContext());listBody.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(getContext());scroll.addView(listBody);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(panel);
        Window w=getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout(dp(320),dp(400));w.setDimAmount(.45f);}
        File root=listener.treeRootDir();
        if(root!=null&&root.canRead()){open(root);}
        else{
            TextView hint=text("尚未授权系统目录。可直接浏览应用目录，或点击下方按钮授权完整文件系统。",size-1,secondary);hint.setPadding(dp(4),dp(6),dp(4),dp(10));panel.addView(hint,1);
            TextView useApp=text("浏览应用目录",16,accent);useApp.setTypeface(Typeface.DEFAULT,Typeface.BOLD);useApp.setGravity(Gravity.CENTER);
            GradientDrawable g=new GradientDrawable();g.setColor(Color.argb(70,216,255,103));g.setCornerRadius(dp(24));useApp.setBackground(g);
            LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(-1,dp(50));glp.topMargin=dp(8);
            panel.addView(useApp,glp);
            useApp.setOnClickListener(v->dismissWith(()->listener.onUseAppDir()));
            TextView grant=text("授权系统目录",14,accent);grant.setGravity(Gravity.CENTER);grant.setPadding(0,dp(6),0,0);panel.addView(grant);
            grant.setOnClickListener(v->dismissWith(()->listener.requestTreeGrant()));
        }
    }
    private void dismissWith(Runnable then){dismiss();then.run();}
    private void open(File dir){
        current=dir;listBody.removeAllViews();
        pathLabel.setText(dir.getAbsolutePath());
        File[] files=dir.listFiles();
        List<File> dirs=new ArrayList<>(),docs=new ArrayList<>();
        if(files!=null){for(File f:files){if(f.isDirectory())dirs.add(f);else docs.add(f);}}
        Collections.sort(dirs);Collections.sort(docs);
        if(dir.getParentFile()!=null){
            listBody.addView(row("📁 ..",null,()->open(dir.getParentFile())));
        }
        for(File d:dirs)listBody.addView(row("📁 "+d.getName(),null,()->open(d)));
        for(File f:docs)listBody.addView(row("📄 "+f.getName(),f,()->showActions(f)));
        if(dirs.isEmpty()&&docs.isEmpty())listBody.addView(text("（空目录）",size-1,secondary));
    }
    private void showActions(File f){
        new AlertDialog.Builder(getContext())
            .setTitle(f.getName())
            .setItems(new String[]{"发给 AI 分析","重命名","删除"},(d,w)->{
                if(w==0){dismiss();listener.onPickTextFile(f);}
                else if(w==1){
                    LinearLayout box=new LinearLayout(getContext());box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(8),dp(16),dp(8));
                    EditText input=new EditText(getContext());input.setSingleLine(true);input.setText(f.getName());
                    box.addView(input);
                    new AlertDialog.Builder(getContext()).setTitle("重命名").setView(box)
                        .setNegativeButton("取消",null)
                        .setPositiveButton("保存",(d2,w2)->{
                            String nn=input.getText().toString().trim();
                            if(!nn.isEmpty()){File nf=new File(f.getParentFile(),nn);if(f.renameTo(nf))open(current);}
                        }).show();
                }
                else{f.delete();open(current);}
            }).show();
    }

    private LinearLayout row(String label,final File f,Runnable onClick){
        LinearLayout row=new LinearLayout(getContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12),dp(9),dp(12),dp(9));
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.argb(30,255,255,255));bg.setCornerRadius(dp(14));row.setBackground(bg);
        TextView t=text(label,size-1,fg);t.setSingleLine(true);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        if(f!=null){
            TextView send=text("→AI",size-2,accent);send.setTypeface(Typeface.DEFAULT,Typeface.BOLD);send.setPadding(dp(8),0,0,0);row.addView(send);
            send.setOnClickListener(v->{dismiss();listener.onPickTextFile(f);});
            TextView del=text("×",size,secondary);del.setPadding(dp(10),0,0,0);row.addView(del);
            del.setOnClickListener(v->{f.delete();open(current);});
        }
        row.setOnClickListener(v->onClick.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(6);
        LinearLayout outer=new LinearLayout(getContext());outer.setOrientation(LinearLayout.VERTICAL);outer.addView(row,lp);
        return outer;
    }
    private TextView text(String s,int sp,int color){TextView v=new TextView(getContext());v.setText(s);v.setTextSize(sp);v.setTextColor(color);return v;}
    private int dp(float v){return (int)(v*getContext().getResources().getDisplayMetrics().density+.5f);}
}
