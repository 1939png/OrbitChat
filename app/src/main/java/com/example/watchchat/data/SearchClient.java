package com.example.watchchat.data;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Synchronous worker-only search. The service must enable SearXNG's JSON format. */
public final class SearchClient {
    public interface Cancellation extends CancelHandle {
        void attach(HttpURLConnection connection) throws IOException;
        void detach(HttpURLConnection connection);
    }
    public static void validateEndpoint(String endpoint) throws IllegalArgumentException {
        try {
            URI u = new URI(endpoint);
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null || u.getUserInfo() != null || u.getFragment() != null)
                throw new IllegalArgumentException("搜索服务地址须为 HTTPS，且不含账号或片段");
        } catch (URISyntaxException e) { throw new IllegalArgumentException("搜索服务地址格式无效"); }
    }
    public String search(String question, RequestOptions options, Cancellation cancel) throws Exception {
        if (options.searchEndpoint.isEmpty()) throw new IOException("尚未配置搜索服务，请在设置填写 SearXNG JSON 地址");
        validateEndpoint(options.searchEndpoint);
        if (question == null || question.trim().isEmpty()) throw new IOException("没有可检索的用户问题");
        URI endpoint = new URI(options.searchEndpoint);
        StringBuilder query = new StringBuilder();
        if (endpoint.getRawQuery() != null) for (String part : endpoint.getRawQuery().split("&")) {
            String key = URLDecoder.decode(part.split("=", 2)[0], "UTF-8");
            if (!"q".equals(key) && !"format".equals(key) && !part.isEmpty()) query.append(part).append('&');
        }
        query.append("q=").append(URLEncoder.encode(limit(question, 2000), "UTF-8")).append("&format=json");
        String base = options.searchEndpoint.split("\\?", 2)[0];
        HttpURLConnection connection = (HttpURLConnection) new URL(base + "?" + query).openConnection();
        try {
            cancel.attach(connection);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(12000); connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            if (!options.searchToken.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + options.searchToken);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IOException("搜索 HTTP " + status + "；请检查地址、凭证及服务端 JSON 配置");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (cancel.isCancelled()) throw new InterruptedIOException("cancelled");
                    if (bytes.size() + n > 512 * 1024) throw new IOException("搜索响应过大");
                    bytes.write(buffer, 0, n);
                }
            }
            JSONObject root;
            try { root = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8)); }
            catch (org.json.JSONException e) { throw new IOException("搜索服务未返回 JSON；请在 SearXNG 启用 JSON 格式"); }
            JSONArray results = root.optJSONArray("results");
            if (results == null) throw new IOException("搜索 JSON 缺少 results，请确认使用 SearXNG 搜索端点");
            StringBuilder reference = new StringBuilder(); int count = 0;
            for (int i = 0; i < results.length() && count < 5; i++) {
                JSONObject item = results.optJSONObject(i); if (item == null) continue;
                String url = item.optString("url", "");
                try { URI u = new URI(url); if (u.getHost() == null || !("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme()))) continue; }
                catch (Exception ignored) { continue; }
                reference.append("[").append(++count).append("] ").append(limit(item.optString("title", ""), 180))
                    .append("\n来源：").append(limit(url, 1000)).append("\n摘要：")
                    .append(limit(item.optString("content", ""), 700)).append("\n\n");
            }
            if (count == 0) throw new IOException("搜索未返回可用结果");
            return reference.toString();
        } finally { cancel.detach(connection); connection.disconnect(); }
    }
    private static String limit(String text, int max) {
        if (text.length() <= max) return text;
        int end = max; if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end);
    }
}
