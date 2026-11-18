package com.example.watchchat.plugin;
/** Optional model/provider extension. */
public interface ModelProvider { String getId(); String getDisplayName(); void complete(ModelRequest request,ModelCallback callback); interface ModelCallback { void onSuccess(String text); void onError(Throwable error); } }
