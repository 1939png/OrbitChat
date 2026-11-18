# Native Android Integration Contract

## Project boundary

- Package and application ID: `com.example.watchchat`.
- Native Java, platform Android Views, API 26+, compile SDK 35, target SDK 28. No Kotlin, Compose, AndroidX, Agent workflows, or external runtime libraries.
- Network client: `com.example.watchchat.data.ChatClient`; persistence: `ChatConfigStore` and `SessionRepository`.
- Intended hardware: 1:1 watch displays and compact/low-memory Android devices. Keep layouts scrollable, controls touch-sized, and work off the UI thread.

## Provider configuration

Required values are provider, base URL, API key, and model. API keys are stored in app-private SharedPreferences and must never be logged or included in exported chat text. The UI must reject an empty key before sending; `ChatClient` also validates it.

Endpoint normalization accepts the service root, a URL ending in `/v1`, or the full provider endpoint. Resulting endpoints are:

- OpenAI-compatible: `{base}/v1/chat/completions` (or `{base}/chat/completions` when base already ends in `/v1`). Auth uses `Authorization: Bearer ...`; body includes `model`, `messages`, and `stream: true`.
- Anthropic: `{base}/v1/messages` (or `{base}/messages` when base already ends in `/v1`). Auth uses `x-api-key` and `anthropic-version`; body includes `model`, `messages`, `max_tokens`, and `stream: true`. System messages are sent through Anthropic's top-level `system` field.

## Threading and request lifecycle

`ChatClient.send(history, config, listener)` starts network I/O on its executor. `ChatListener` callbacks run on that worker thread; UI updates must be marshalled to the main thread. The returned `CancelHandle` disconnects the active request. Streaming deltas append in order. Completion persists the full assistant response; cancellation and terminal errors preserve any received partial response. Transient HTTP 429/5xx and transport errors may retry up to configured `maxRetries`; invalid client errors surface immediately.

`SessionRepository` is synchronous by design. Every call (`createSession`, `appendMessage`, `listSessions`, `getMessages`, `searchMessages`, `exportSession`, `deleteSession`) must run on a worker thread. Writes keep session timestamps and messages consistent. Search treats `%`, `_`, and backslash in user queries literally.

## Acceptance Criteria

### Build and packaging

- [ ] `gradlew.bat :app:assembleDebug` succeeds with JDK 17, Gradle 8.9, Android SDK Platform 35, and AGP 8.7.3.
- [x] All Java sources compile against Android 35 `android.jar` with Java 8 source/target compatibility (independent javac check).
- [ ] Manifest launcher resolves to `com.example.watchchat.MainActivity`; install and launch on API 26 and a current Android API.
- [ ] No network, database, or preference disk operation runs on the main thread.

### Provider API

- [ ] Configure each provider with a valid key/model; verify root, `/v1`, and full endpoint URL forms produce exactly one `/v1` and one provider path.
- [ ] Verify OpenAI-compatible SSE and Anthropic SSE deltas render in order, including multi-line answers and UTF-8 text.
- [ ] Verify 401/400 errors are actionable and are not retried; 429/5xx and transient transport errors obey retry limits.
- [ ] Stop during streaming; verify the request disconnects, stop control returns to send, and partial text persists once.
- [ ] Verify missing API key blocks sending, and changing provider/settings affects the very next request.

### Persistence and chat workflows

- [ ] Create/switch/delete sessions; ensure deleting the active session selects or creates a valid session.
- [ ] Restart app; verify sessions, message order, and timestamps persist.
- [ ] Search messages with normal text and `%`, `_`, and `\`; verify literal matching and bounded results.
- [ ] Share the current conversation as plain text; confirm API credentials are absent.
- [ ] Verify long conversations stay responsive while scrolling, sending, searching, and deleting.

### Device and display matrix

- [ ] Square 1:1 emulator/device at 240x240 and 320x320: onboarding/empty state, settings, composer, keyboard, history, search, and long-message scrolling have no clipped or overlapping controls.
- [ ] Android 8/API 26 low-memory device: launch, SQLite operations, TLS/network request, streaming, cancellation, rotation/recreation behavior, and repeated session navigation.
- [ ] Compact non-square phone at 360x640: controls remain usable and content scrolls without overlap.
- [ ] Validate touch targets, readable text at device density, dark theme contrast, and reduced-motion/slow-device behavior.

## Verification status

Java compilation is verified; Gradle APK assembly and all runtime/network/device acceptance items remain unverified. The current workspace has no attached Android device or emulator.
