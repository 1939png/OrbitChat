from pathlib import Path
p=Path(r'D:\对话\app\src\main\java\com\example\watchchat\MainActivity.java')
s=p.read_text(encoding='utf-8')
s=s.replace('        try{active=client.send(history,requestConfig,requestOptions,new ChatListener(){', '        try{active=client.send(history,requestConfig,requestOptions,new ChatListener(){')
s=s.replace('            public void onStatus(String status){runOnUiThread', '            public void onUsage(ChatUsage usage){storage.execute(() -> repository.recordUsage(target, usage));}\n            public void onStatus(String status){runOnUiThread')
needle='    private void showSessions(){'
helper='''    private String stat(Integer value){return value==null?"未知":String.valueOf(value);}
    private String formatUpdated(long value){return value<=0?"未知":new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(value));}
    private void showSessionInfo(SessionRepository.Session s){
        String text="缓存命中："+(s.cacheHit==null?"未知":(s.cacheHit?"是":"否"))+"\n上下文占用："+stat(s.contextTokens)+" tokens\n输入 token："+stat(s.inputTokens)+"\n输出 token："+stat(s.outputTokens)+"\n请求次数："+stat(s.requestCount)+"\n最后更新时间："+formatUpdated(s.updatedAt);
        new AlertDialog.Builder(this).setTitle(s.title).setMessage(text).setPositiveButton("确定",null).show();
    }
'''
s=s.replace(needle,helper+needle)
old='name.setOnClickListener(v->loadSession(s.id));name.setOnLongClickListener(v->{renameSession(s,name);return true;});r.setOnLongClickListener(v->{renameSession(s,name);return true;});'
new='name.setOnClickListener(v->loadSession(s.id));name.setOnLongClickListener(v->{renameSession(s,name);return true;});r.setOnLongClickListener(v->{showSessionInfo(s);return true;});'
if old not in s: raise SystemExit('history binding not found')
s=s.replace(old,new)
p.write_text(s,encoding='utf-8')
