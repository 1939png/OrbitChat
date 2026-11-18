package com.example.watchchat.plugin;
public final class ModelRequest { private final String prompt; public ModelRequest(String prompt){if(prompt==null)throw new IllegalArgumentException("prompt is required");this.prompt=prompt;} public String getPrompt(){return prompt;} }
