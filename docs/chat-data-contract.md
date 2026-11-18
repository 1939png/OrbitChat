# Chat data module contract

Package: `com.example.watchchat.data`, Android API 26+, Java. Uses platform SQLite/SharedPreferences, `org.json`, and `HttpURLConnection`; no external runtime libraries.

## UI integration

`MainActivity` uses data classes directly; there is no mapping through `ChatModels` and no facade. Network and persistence classes are `ChatClient`, `ChatConfig`, `ChatMessage`, `ChatListener`, `CancelHandle`, `ChatConfigStore`, and `SessionRepository`.

- `ChatClient.send(history, config, listener)` returns a `CancelHandle`; listener callbacks run on the network worker and UI updates must use the main thread.
- `ChatConfigStore.load()/save(config)` persists provider, Base URL, model, and API key in app-private SharedPreferences.
- `SessionRepository` exposes `createSession`, `appendMessage`, `listSessions`, `getMessages`, `searchMessages`, `exportSession`, and `deleteSession`. SQLite APIs are synchronous; invoke them on a worker thread.

## Protocols

OpenAI-compatible: POST `{base}/v1/chat/completions`, Bearer auth, body `stream: true`, consumes `choices[0].delta.content` and `[DONE]`. Anthropic: POST `{base}/v1/messages`, `x-api-key` plus `anthropic-version: 2023-06-01`, moves system messages to the top-level `system` field and consumes `content_block_delta.delta.text`. Input Base URLs may be service roots, end in `/v1`, or already include the full endpoint path. HTTP 429/5xx and transport failures use bounded retry; other HTTP failures surface as `ChatError`. Cancellation disconnects the active `HttpURLConnection`.

## Persistence

Messages are ordered by creation time and ID. Message insertion and session timestamp updates share a database transaction. Session deletion removes associated messages transactionally. Search escapes LIKE metacharacters and limits result count. `exportSession` returns plain text without provider credentials.

## Verification

All Java sources compile against Android SDK 35 `android.jar` using JDK 25 with Java 8 source/target compatibility. Gradle APK assembly, fake-endpoint protocol tests, Android instrumentation tests, and runtime/device checks have not run. See [CONTRACT.md](../CONTRACT.md) for the complete provider, persistence, build, and square-device acceptance matrix.
