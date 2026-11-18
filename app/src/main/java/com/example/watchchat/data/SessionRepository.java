package com.example.watchchat.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Synchronous SQLite repository; call from a worker thread. */
public final class SessionRepository extends SQLiteOpenHelper {
    private static final int DB_VERSION = 5;
    /** A title is a label, not content: long enough for a full sentence, short enough
     *  to render on one line of a 500px watch without invisible truncation. */
    private static final int MAX_TITLE_LENGTH = 80;
    public static final class Session {
        public final String id,title; public final long updatedAt;
        public final Integer cacheHit,contextTokens,inputTokens,outputTokens,requestCount,cacheHitTokens;
        /** Wall-clock duration of the last request, ms. 0 when never recorded. */
        public final long elapsedMs;
        public final boolean latestRecorded, countSinceUpgrade;
        Session(String i,String t,long u,Integer ch,Integer ct,Integer in,Integer out,Integer rc,boolean recorded,boolean since){this(i,t,u,ch,ct,in,out,rc,recorded,since,null,0L);}
        Session(String i,String t,long u,Integer ch,Integer ct,Integer in,Integer out,Integer rc,boolean recorded,boolean since,Integer cht){this(i,t,u,ch,ct,in,out,rc,recorded,since,cht,0L);}
        Session(String i,String t,long u,Integer ch,Integer ct,Integer in,Integer out,Integer rc,boolean recorded,boolean since,Integer cht,long el){id=i;title=t;updatedAt=u;cacheHit=ch;contextTokens=ct;inputTokens=in;outputTokens=out;requestCount=rc;latestRecorded=recorded;countSinceUpgrade=since;cacheHitTokens=cht;elapsedMs=el;}
    }
    public SessionRepository(Context c){super(c.getApplicationContext(),"chat_sessions.db",null,DB_VERSION);setWriteAheadLoggingEnabled(true);}
    public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE sessions(id TEXT PRIMARY KEY,title TEXT NOT NULL,updated_at INTEGER NOT NULL,cache_hit INTEGER,context_tokens INTEGER,input_tokens INTEGER,output_tokens INTEGER,request_count INTEGER NOT NULL DEFAULT 0,request_key TEXT,latest_recorded INTEGER NOT NULL DEFAULT 0,count_since_upgrade INTEGER NOT NULL DEFAULT 0,cache_hit_tokens INTEGER,elapsed_ms INTEGER)");
        db.execSQL("CREATE TABLE messages(id TEXT PRIMARY KEY,session_id TEXT NOT NULL,role TEXT NOT NULL,content TEXT NOT NULL,created_at INTEGER NOT NULL,FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX messages_session_time ON messages(session_id,created_at)"); db.execSQL("CREATE INDEX messages_content ON messages(content)");
    }
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2){
            db.execSQL("ALTER TABLE sessions ADD COLUMN cache_hit INTEGER");
            db.execSQL("ALTER TABLE sessions ADD COLUMN context_tokens INTEGER");
            db.execSQL("ALTER TABLE sessions ADD COLUMN input_tokens INTEGER");
            db.execSQL("ALTER TABLE sessions ADD COLUMN output_tokens INTEGER");
            db.execSQL("ALTER TABLE sessions ADD COLUMN request_count INTEGER NOT NULL DEFAULT 0");
        }
        if(oldVersion<3){
            db.execSQL("ALTER TABLE sessions ADD COLUMN request_key TEXT");
            db.execSQL("ALTER TABLE sessions ADD COLUMN latest_recorded INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE sessions ADD COLUMN count_since_upgrade INTEGER NOT NULL DEFAULT 1");
            // v2 stored a per-event constant, not a request count; its context used total_tokens.
            db.execSQL("UPDATE sessions SET request_count=0,context_tokens=input_tokens");
        }
        if(oldVersion<4){
            db.execSQL("ALTER TABLE sessions ADD COLUMN cache_hit_tokens INTEGER");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN elapsed_ms INTEGER");
        }
    }
    // One user send owns a key across retries and SSE chunks. New sends clear stale usage.
    static final String BEGIN_REQUEST_SQL = "UPDATE sessions SET request_count=COALESCE(request_count,0)+1,request_key=?,latest_recorded=1,cache_hit=NULL,context_tokens=NULL,input_tokens=NULL,output_tokens=NULL,updated_at=? WHERE id=?";
    public String beginRequest(String sessionId){
        String key=UUID.randomUUID().toString();
        getWritableDatabase().execSQL(BEGIN_REQUEST_SQL,new Object[]{key,System.currentTimeMillis(),sessionId});
        return key;
    }
    /** Trim, fall back for empty, and clamp to the column's practical limit. */
    private static String clampTitle(String raw, String fallback){
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return fallback;
        if (value.length() > MAX_TITLE_LENGTH) {
            value = value.substring(0, MAX_TITLE_LENGTH - 1) + "\u2026";
        }
        return value;
    }

    public String createSession(String title){String id=UUID.randomUUID().toString();ContentValues v=new ContentValues();v.put("id",id);v.put("title",clampTitle(title,"New chat"));v.put("updated_at",System.currentTimeMillis());v.put("count_since_upgrade",0);getWritableDatabase().insertOrThrow("sessions",null,v);return id;}
    public void appendMessage(String sessionId,ChatMessage m){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("id",m.id==null?UUID.randomUUID().toString():m.id);v.put("session_id",sessionId);v.put("role",m.role.name());v.put("content",m.content);v.put("created_at",m.createdAt);db.beginTransaction();try{db.insertOrThrow("messages",null,v);ContentValues u=new ContentValues();u.put("updated_at",m.createdAt);db.update("sessions",u,"id=?",new String[]{sessionId});db.setTransactionSuccessful();}finally{db.endTransaction();}}
    /** Overload for callers that do not time the request. */
    public void recordUsage(String sessionId,String requestKey,ChatUsage usage){
        recordUsage(sessionId,requestKey,usage,0L);
    }

    public void recordUsage(String sessionId,String requestKey,ChatUsage usage,long elapsedMs){
        if(sessionId==null||requestKey==null||usage==null)return;
        ContentValues v=new ContentValues();
        putNullable(v,"cache_hit",usage.cacheHit==null?null:usage.cacheHit?1:0);
        putNullable(v,"cache_hit_tokens",usage.cachedInputTokens);
        putNullable(v,"context_tokens",usage.contextTokens);putNullable(v,"input_tokens",usage.inputTokens);putNullable(v,"output_tokens",usage.outputTokens);
        // Only overwrite the stored duration when this request actually measured one.
        if (elapsedMs > 0) v.put("elapsed_ms", elapsedMs);
        getWritableDatabase().update("sessions",v,"id=? AND request_key=?",new String[]{sessionId,requestKey});
    }
    private static void putNullable(ContentValues values,String key,Integer value){if(value==null)values.putNull(key);else values.put(key,value);}
    private static final String SESSION_COLUMNS="id,title,updated_at,cache_hit,context_tokens,input_tokens,output_tokens,request_count,latest_recorded,count_since_upgrade,cache_hit_tokens,elapsed_ms";
    private static Session readSession(Cursor c){return new Session(c.getString(0),c.getString(1),c.getLong(2),nullableInt(c,3),nullableInt(c,4),nullableInt(c,5),nullableInt(c,6),nullableInt(c,7),c.getInt(8)!=0,c.getInt(9)!=0,nullableInt(c,10),c.isNull(11)?0L:c.getLong(11));}
    public Session getSession(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT "+SESSION_COLUMNS+" FROM sessions WHERE id=?",new String[]{id})){return c.moveToFirst()?readSession(c):null;}}
    public List<Session> listSessions(){List<Session> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT "+SESSION_COLUMNS+" FROM sessions ORDER BY updated_at DESC",null)){while(c.moveToNext())out.add(readSession(c));}return out;}
    private static Integer nullableInt(Cursor c,int i){return c.isNull(i)?null:c.getInt(i);}
    public List<ChatMessage> getMessages(String sessionId){List<ChatMessage> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,role,content,created_at FROM messages WHERE session_id=? ORDER BY created_at,id",new String[]{sessionId});try{while(c.moveToNext())out.add(new ChatMessage(c.getString(0),ChatMessage.Role.valueOf(c.getString(1)),c.getString(2),c.getLong(3)));}finally{c.close();}return out;}
    public List<ChatMessage> searchMessages(String query,int limit){List<ChatMessage> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,role,content,created_at FROM messages WHERE content LIKE ? ESCAPE '\\' ORDER BY created_at DESC LIMIT ?",new String[]{"%"+escapeLike(query)+"%",String.valueOf(Math.max(1,limit))});try{while(c.moveToNext())out.add(new ChatMessage(c.getString(0),ChatMessage.Role.valueOf(c.getString(1)),c.getString(2),c.getLong(3)));}finally{c.close();}return out;}
    private static String escapeLike(String s){return s.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
    public String exportSession(String id){StringBuilder b=new StringBuilder();for(ChatMessage m:getMessages(id))b.append(m.role.name()).append(":\n").append(m.content).append("\n\n");return b.toString();}
    public boolean renameSession(String id,String title){String value=clampTitle(title,"");if(value.isEmpty())throw new IllegalArgumentException("Empty session title");ContentValues update=new ContentValues();update.put("title",value);return getWritableDatabase().update("sessions",update,"id=?",new String[]{id})==1;}
    public void deleteSession(String id){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("messages","session_id=?",new String[]{id});db.delete("sessions","id=?",new String[]{id});db.setTransactionSuccessful();}finally{db.endTransaction();}}
}
