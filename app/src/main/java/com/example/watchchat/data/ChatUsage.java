package com.example.watchchat.data;

/** A nullable snapshot of one provider attempt; never a sum of streaming events. */
public final class ChatUsage {
    public final Boolean cacheHit;
    public final Integer contextTokens, inputTokens, outputTokens, cachedInputTokens, cacheCreationTokens;
    private final Integer rawInputTokens;
    private final boolean anthropic;

    private ChatUsage(boolean anthropic,Integer rawInput,Integer output,Integer cached,Integer created) {
        this.anthropic=anthropic;this.rawInputTokens=rawInput;
        this.outputTokens=output;this.cachedInputTokens=cached;this.cacheCreationTokens=created;
        this.cacheHit=cached==null?null:cached>0;
        // Anthropic separates uncached input from cache reads and cache creation.
        this.inputTokens=rawInput==null?null:anthropic?sum(rawInput,cached,created):rawInput;
        this.contextTokens=this.inputTokens;
    }
    public static ChatUsage partial(boolean anthropic,Integer input,Integer output,Integer cached,Integer created) {
        return new ChatUsage(anthropic,input,output,cached,created);
    }
    public static ChatUsage unknown(){return partial(false,null,null,null,null);}
    public ChatUsage merge(ChatUsage next){
        if(next==null)return this;
        if(anthropic!=next.anthropic)return next;
        return partial(anthropic,pick(rawInputTokens,next.rawInputTokens),pick(outputTokens,next.outputTokens),
            pick(cachedInputTokens,next.cachedInputTokens),pick(cacheCreationTokens,next.cacheCreationTokens));
    }
    private static Integer pick(Integer old,Integer next){return next==null?old:next;}
    private static Integer sum(int input,Integer cached,Integer created){
        long total=(long)input+(cached==null?0:cached)+(created==null?0:created);
        return total<=Integer.MAX_VALUE?(int)total:null;
    }
}
