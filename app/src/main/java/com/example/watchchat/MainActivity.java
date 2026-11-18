/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.example.watchchat.R$style
 */
package com.example.watchchat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.watchchat.AppearancePreferences;
import com.example.watchchat.AssistantMessageView;
import com.example.watchchat.FileBrowser;
import com.example.watchchat.FontStore;
import com.example.watchchat.ImagePickerDialog;
import com.example.watchchat.LogoView;
import com.example.watchchat.R;
import com.example.watchchat.ReasoningSlider;
import com.example.watchchat.SendButton;
import com.example.watchchat.SessionInfoPanel;
import com.example.watchchat.SettingsPage;
import com.example.watchchat.SplashView;
import com.example.watchchat.VisualEffects;
import com.example.watchchat.data.Attachment;
import com.example.watchchat.data.CancelHandle;
import com.example.watchchat.data.ChatClient;
import com.example.watchchat.data.ChatConfig;
import com.example.watchchat.data.ChatConfigStore;
import com.example.watchchat.data.ChatError;
import com.example.watchchat.data.ChatListener;
import com.example.watchchat.data.ChatMessage;
import com.example.watchchat.data.ChatUsage;
import com.example.watchchat.data.ModelSettings;
import com.example.watchchat.data.ProviderStore;
import com.example.watchchat.data.RequestOptions;
import com.example.watchchat.data.SessionRepository;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity
extends Activity {
    private static final int BG = -16777216;
    private static final int PANEL = Color.rgb(16, 16, 16);
    private static final int WHITE = Color.rgb(250, 250, 250);
    private static final int MUTED = Color.rgb(150, 150, 150);
    private final List<ChatMessage> messages = new ArrayList<ChatMessage>();
    private final Map<String, String> reasoningByMessage = new HashMap<String, String>();
    private final ExecutorService storage = Executors.newSingleThreadExecutor();
    private SessionRepository repository;
    private ChatClient client;
    private ChatConfigStore configStore;
    private ChatConfig config;
    private CancelHandle active;
    private LinearLayout root;
    private LinearLayout messageColumn;
    private ScrollView scroll;
    private EditText composer;
    private AssistantMessageView streamBody;
    private TextView thinkingBody;
    private TextView activityStatus;
    /** Settled pixel mark in the chat header; the boot mark flies into this slot. */
    private SplashView headerLogo;
    /** The mark that rests in the middle of an empty conversation. */
    private SplashView centerMark;
    private LinearLayout.LayoutParams centreMarkLp;
    /** The boot overlay, kept so dismissal can fly it into the header. */
    private SplashView splash;
    private LinearLayout thinkingWrap;
    private String sessionId;
    private boolean generating;
    private boolean reasoningExpanded;
    private float downX;
    private float downY;
    private int scale = 1;
    private boolean highContrast;
    private final Handler startupHandler = new Handler(Looper.getMainLooper());
    private long startupShownAt;
    private TextView sendButton;
    private TextView bottomButton;
    private FrameLayout chatArea;
    private FrameLayout reasoningOverlay;
    private View loadingIndicator;
    private View reasoningControls;
    private boolean followLatest = true;
    private boolean chatVisible;
    private boolean reasoningPanelOpen;
    private boolean touchingChat;
    private boolean suppressSwipe;
    private float pageFrom;
    private static final int PAGE_SETTINGS = 0;
    private static final int PAGE_CHAT = 1;
    private static final int PAGE_HISTORY = 2;
    private FrameLayout screen;
    private FrameLayout pager;
    private final View[] pageSlots = new View[3];
    private int currentPageIndex = 1;
    private float camDownX;
    private float camDownY;
    private float camLastX;
    private float camVel;
    private long camLastT;
    private boolean camTracking;
    private boolean camDecided;
    private boolean camApplied;
    private boolean camYieldsToChild;
    private View splashOverlay;
    private SettingsPage settingsPage;
    private SettingsPage.Host settingsHost;
    private List<SessionRepository.Session> cachedSessions = new ArrayList<SessionRepository.Session>();
    private boolean historyVisible;
    private int readingY;
    private String displayedSession;
    private String requestTarget;
    private String answerRaw = "";
    private String thoughtRaw = "";
    private long requestSerial;
    private String composerDraft = "";
    private ReasoningSlider reasoningSlider;
    private String[] reasoningValues = new String[0];
    private String searchNotice = "";
    private float chatTouchY;
    private static final int PICK_IMAGE = 801;
    private static final int CREATE_OUTPUT = 803;
    private static final int PICK_TREE = 804;
    private static final int PICK_WALLPAPER = 805;
    private final Map<String, Attachment> imageAttachments = new HashMap<String, Attachment>();
    private final List<String> pendingImageNames = new ArrayList<String>();
    private String pendingSaveContent = "";
    private File safRootDir;
    private Uri safTreeUri;
    private FileBrowser fileBrowser;
    private static final float COMMIT_FRACTION = 0.28f;
    private static final float FLING_DP_PER_SEC = 300.0f;
    private static final float MIN_FLING_DISTANCE_DP = 18.0f;
    private VelocityTracker velocityTracker;
    private int camSlop;
    private View welcomeView;
    private View chatChrome;
    private ImagePickerDialog openPicker;

    private boolean ownsRequest(long serial) {
        return this.generating && serial == this.requestSerial && !this.isFinishing() && !this.isDestroyed();
    }

    private boolean inMessageColumn(View view) {
        if (view == null || this.messageColumn == null) {
            return false;
        }
        ViewParent parent = view.getParent();
        while (parent instanceof View) {
            if (parent == this.messageColumn) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void rememberReading() {
        if (this.chatVisible && this.scroll != null) {
            this.readingY = this.scroll.getScrollY();
            if (this.composer != null) {
                this.composerDraft = this.composer.getText().toString();
            }
        }
    }

    /** Called when a surface opens over the chat: the affordance would float on top of it. */
    private void hideBottomButton() {
        if (this.bottomButton != null) this.bottomButton.setVisibility(8);
    }

    private void updateBottomButton() {
        if (this.bottomButton == null || this.scroll == null || this.messageColumn == null) {
            return;
        }
        // Derive visibility from the real scroll position. The old test also consulted
        // followLatest, which is sticky: once it latched false the button never hid again,
        // even at the very bottom or with the panel open.
        int bottom = this.scroll.getScrollY() + this.scroll.getHeight();
        int content = this.messageColumn.getHeight();
        boolean away = bottom < content - this.dp(40.0f);
        // While the user is actively dragging the list, keep their own intent rather than
        // flickering the button on and off mid-gesture.
        this.bottomButton.setVisibility(away ? 0 : 8);
    }

    private void followIfAllowed() {
        ScrollView pane = this.scroll;
        if (pane == null) {
            return;
        }
        pane.post(() -> {
            if (this.chatVisible && pane == this.scroll && this.followLatest && !this.touchingChat) {
                pane.scrollTo(0, Math.max(0, this.messageColumn.getHeight() - pane.getHeight()));
            }
            if (pane == this.scroll) {
                this.updateBottomButton();
            }
        });
    }

    private void updateRequestChrome() {
        if (!this.chatVisible) {
            return;
        }
        if (this.sendButton != null) {
            this.sendButton.setText(this.generating ? "\u25a0" : "\u2191");
            this.sendButton.setContentDescription(this.generating ? "\u505c\u6b62\u751f\u6210" : "\u53d1\u9001\u6d88\u606f");
        }
        if (this.loadingIndicator != null) {
            this.loadingIndicator.setVisibility(this.generating ? 0 : 8);
        }
        if (this.activityStatus != null && !this.generating) {
            this.activityStatus.setText(this.searchNotice.isEmpty() ? "" : this.searchNotice);
        }
    }

    private String reasoningPreferenceKey() {
        return "reasoning:" + (Object)((Object)this.config.provider) + ":" + this.config.baseUrl + ":" + this.config.model;
    }

    private String selectedReasoning() {
        ProviderStore.Provider ap = ProviderStore.activeProvider(this);
        if (ap == null) {
            return "";
        }
        String model = ProviderStore.activeModel(this);
        // The Context-aware overload is required here: the no-arg one cannot see custom
        // suffixes, so a user-defined one would silently fall back to the built-in array.
        return ModelSettings.get(this, ap.id, model).reasoningValue(this, ap.id, model);
    }

    private int selectedMaxOutput() {
        ProviderStore.Provider ap = ProviderStore.activeProvider(this);
        if (ap == null) {
            return 4096;
        }
        return ModelSettings.get(this, ap.id, ProviderStore.activeModel(this)).outputTokens();
    }

    private void rebuildReasoningRow() {
        if (this.composer == null || this.composer.getParent() == null) {
            return;
        }
        View bar = this.reasoningControls;
        if (bar == null) {
            return;
        }
        ViewParent parent = bar.getParent();
        if (!(parent instanceof ViewGroup)) {
            return;
        }
        ViewGroup host = (ViewGroup)parent;
        int index = host.indexOfChild(bar);
        if (index < 0) {
            return;
        }
        // Rebuilding the row resets the panel to its default collapsed state, so the
        // user was bounced out of the panel they were working in. Carry the open flag
        // across the rebuild and re-open it in place.
        boolean wasOpen = this.reasoningPanelOpen;
        host.removeViewAt(index);
        this.addReasoningControlsAt(host, index);
        if (wasOpen) this.reasoningPanelOpen = true;
    }

    private void addReasoningControls(LinearLayout box) {
        LinearLayout container = this.col();
        this.reasoningControls = container;
        this.reasoningSlider = null;
        this.reasoningValues = new String[0];
        ProviderStore.Provider ap = ProviderStore.activeProvider(this);
        if (ap == null) {
            container.setVisibility(8);
            box.addView((View)container, new LinearLayout.LayoutParams(-1, -2));
            return;
        }
        String pid = ap.id;
        String model = ProviderStore.activeModel(this);
        Set<Integer> enabled = ModelSettings.getEnabledReasoning(this, pid, model);
        LinearLayout panel = this.col();
        panel.setPadding(this.dp(14.0f), this.dp(12.0f), this.dp(14.0f), this.dp(14.0f));
        GradientDrawable panelBg = new GradientDrawable();
        boolean lightPanel = AppearancePreferences.lightMode(this);
        panelBg.setColor(lightPanel ? 0xffffffff : 0xff161616);
        panelBg.setCornerRadius(this.dp(20.0f));
        panelBg.setStroke(Math.max(1, this.dp(1.0f)),
                lightPanel ? 0xffd8d8d4 : 0x33FFFFFF);
        panel.setBackground(panelBg);
        TextView modelName = this.text(model, 18, this.fg());
        modelName.setTypeface(VisualEffects.type(this));
        modelName.setSingleLine(true);
        modelName.setGravity(Gravity.CENTER);
        panel.addView((View)modelName, new LinearLayout.LayoutParams(-1, this.dp(26.0f)));
        modelName.setContentDescription("\u5207\u6362\u6a21\u578b");
                // No filled slab behind the name: a full-width pill reads as a button and
        // competes with the capsule below, so the name carries weight through type alone.
        modelName.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == 0) {
                v.setAlpha(0.6f);
            } else if (ev.getAction() == 1 || ev.getAction() == 3) {
                v.setAlpha(1.0f);
            }
            return false;
        });
        modelName.setOnClickListener(v -> this.showModelPicker());
        TextView effortNow = this.text("", 12, this.muted());
        effortNow.setGravity(Gravity.CENTER);
        panel.addView((View)effortNow, new LinearLayout.LayoutParams(-1, this.dp(20.0f)));
        if (enabled.size() > 1) {
            ArrayList<Integer> order = new ArrayList<Integer>(enabled);
            Collections.sort(order);
            ArrayList<String> labels = new ArrayList<String>();
            ArrayList<String> values = new ArrayList<String>();
            Iterator iterator = order.iterator();
            while (iterator.hasNext()) {
                int i = (Integer)iterator.next();
                labels.add(ModelSettings.labelTextAt(this, pid, model, i));
                values.add(ModelSettings.labelAt(this, pid, model, i));
            }
            this.reasoningValues = values.toArray(new String[0]);
            ArrayList<Integer> finalOrder = order;
            int current = ModelSettings.get((Context)this, (String)pid, (String)model).reasoningIndex;
            int index = 0;
            for (int i = 0; i < finalOrder.size(); ++i) {
                if ((Integer)finalOrder.get(i) != current) continue;
                index = i;
            }
            this.reasoningSlider = new ReasoningSlider(this);
            this.reasoningSlider.setLabels(labels.toArray(new String[0]));
            this.reasoningSlider.setSelectedIndexImmediate(index);
            this.reasoningSlider.setOnLevelChangeListener((view, level, fromUser) -> {
                if (!fromUser || level < 0 || level >= finalOrder.size()) {
                    return;
                }
                ModelSettings.putReasoningIndex(this, pid, model, (Integer)finalOrder.get(level));
                effortNow.setText(ModelSettings.labelTextAt(this, pid, model, (Integer)finalOrder.get(level)) + " \u601d\u8003");
            });
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
            slp.topMargin = this.dp(6.0f);
            panel.addView((View)this.reasoningSlider, slp);
            effortNow.setText(ModelSettings.labelTextAt(this, pid, model, (Integer)finalOrder.get(index)) + " \u601d\u8003");
        } else {
            effortNow.setText(enabled.isEmpty() ? "\u4e0d\u542f\u7528\u601d\u8003" : ModelSettings.labelTextAt(this, pid, model, enabled.iterator().next()) + " \u601d\u8003");
            // The slider is hidden when only one level is enabled. Say why, or the panel
            // looks broken: it shows a title, a level, and nothing to drag.
            TextView hint = this.text(enabled.isEmpty()
                    ? "去模型设置里启用思考档位"
                    : "只启用了一个档位，去模型设置里多勾选几个"
                    , 11, this.muted());
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
            hlp.topMargin = this.dp(2.0f);
            panel.addView((View)hint, hlp);
        }
        TextView chevron = this.text("\u25b2", 13, this.muted());
        chevron.setGravity(17);
        chevron.setContentDescription("\u601d\u8003\u5f3a\u5ea6\u8bbe\u7f6e");
        chevron.setBackground(null);
        LinearLayout trigger = this.col();
        trigger.setGravity(17);
        trigger.addView((View)chevron, new LinearLayout.LayoutParams(this.dp(44.0f), this.dp(22.0f)));
        trigger.setClickable(true);
        trigger.setFocusable(true);
        // Start in the state the user left the panel in: a rebuild (after switching model)
        // must not silently collapse what they were working in.
        boolean startOpen = this.reasoningPanelOpen;
        panel.setVisibility(startOpen ? 0 : 8);
        boolean[] open = new boolean[]{startOpen};
        if (startOpen) chevron.setRotation(180f);
        int panelHeight = this.dp(96.0f);
        trigger.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == 0) {
                chevron.animate().cancel();
                chevron.setAlpha(0.55f);
                chevron.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100L)
                        .setInterpolator(VisualEffects.EASE_OUT).start();
                return true;
            }
            if (ev.getAction() == 1) {
                chevron.animate().cancel();
                chevron.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(140L).setInterpolator(VisualEffects.EASE_OUT).start();
                this.toggleReasoningPanel(panel, chevron, open, panelHeight);
                v.performClick();
                return true;
            }
            if (ev.getAction() == 3) {
                chevron.animate().cancel();
                chevron.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(140L)
                        .setInterpolator(VisualEffects.EASE_OUT).start();
                return true;
            }
            return false;
        });
        container.addView((View)panel, new LinearLayout.LayoutParams(-1, -2));
        container.addView((View)trigger, new LinearLayout.LayoutParams(-1, this.dp(26.0f)));
        container.setVisibility(0);
        // Float the panel OVER the transcript rather than reserving layout height: it is a
        // surface that appears on demand, so chat text must not reflow when it opens.
        // The overlay must not take layout space, so it lives in a FrameLayout that sits
        // behind the page and paints the panel above the transcript.
        if (this.reasoningOverlay != null) {
            this.reasoningOverlay.removeAllViews();
            this.reasoningOverlay.addView((View)container, new FrameLayout.LayoutParams(-1, -2));
        } else {
            box.addView((View)container, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void toggleReasoningPanel(final LinearLayout panel, TextView chevron, boolean[] open, int panelHeight) {
        int to;
        open[0] = !open[0];
        this.reasoningPanelOpen = open[0];
        final boolean opening = open[0];
        chevron.animate().rotation(opening ? 180.0f : 0.0f).setDuration(220L).setInterpolator(VisualEffects.EASE_OUT).start();
        panel.performHapticFeedback(4);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            panel.setVisibility(opening ? 0 : 8);
            return;
        }
        int from = opening ? (panel.getHeight() > 0 ? panel.getHeight() : 0) : panel.getHeight();
        int n = to = opening ? panelHeight : 0;
        if (opening && panel.getVisibility() != 0) {
            panel.setVisibility(0);
            panel.getLayoutParams().height = 0;
            panel.requestLayout();
        }
        ValueAnimator va = ValueAnimator.ofInt(from, to);
        va.setDuration(240L);
        va.setInterpolator(VisualEffects.EASE_OUT);
        va.addUpdateListener(a -> {
            panel.getLayoutParams().height = (Integer)a.getAnimatedValue();
            panel.requestLayout();
        });
        va.addListener(new AnimatorListenerAdapter(){

            @Override
            public void onAnimationEnd(Animator a) {
                panel.getLayoutParams().height = -2;
                panel.requestLayout();
                if (!opening) {
                    panel.setVisibility(8);
                }
            }
        });
        va.start();
    }

    private void addReasoningControlsAt(ViewGroup host, int index) {
        LinearLayout holder = this.col();
        this.addReasoningControls(holder);
        if (holder.getChildCount() > 0) {
            View built = holder.getChildAt(0);
            holder.removeViewAt(0);
            host.addView(built, index, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private boolean touches(View view, MotionEvent event) {
        if (view == null || view.getVisibility() != 0) {
            return false;
        }
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        return event.getRawX() >= (float)pos[0] && event.getRawX() < (float)(pos[0] + view.getWidth()) && event.getRawY() >= (float)pos[1] && event.getRawY() < (float)(pos[1] + view.getHeight());
    }

    private int dp(float n) {
        return (int)(n * this.getResources().getDisplayMetrics().density + 0.5f);
    }

    private int accent() {
        return AppearancePreferences.accent(this);
    }

    private int panel() {
        return AppearancePreferences.panel(this);
    }

    private int fg() {
        // In light mode text is pure black, as asked. In dark mode it stays pure white.
        if (!this.highContrast) return AppearancePreferences.ink(this);
        return AppearancePreferences.lightMode(this) ? 0xff000000 : 0xffffffff;
    }

    private int muted() {
        if (!this.highContrast) return AppearancePreferences.inkMuted(this);
        return AppearancePreferences.lightMode(this) ? 0xff36393f : 0xffdcdcdc;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(this.dp(radius));
        return d;
    }

    private TextView text(String s, int size, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setGravity(16);
        VisualEffects.styleText(v, 0.01f);
        return v;
    }

    private int bodySize() {
        return Math.round(FontStore.bodySize(this));
    }

    private int uiSize() {
        return Math.round(FontStore.uiSize(this));
    }

    private TextView label(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(this.fg());
        VisualEffects.styleText(t, 0.01f);
        return t;
    }

    private TextView label(String value, int size, int color) {
        TextView t = this.label(value, size);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(1);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(0);
        l.setGravity(16);
        return l;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        this.getWindow().setStatusBarColor(-16777216);
        this.getWindow().setNavigationBarColor(-16777216);
        this.repository = new SessionRepository(this);
        this.client = new ChatClient();
        this.configStore = new ChatConfigStore(this);
        this.scale = this.configStore.textScale();
        this.highContrast = this.configStore.highContrast();
        this.startupShownAt = SystemClock.uptimeMillis();
        this.showLoading();
        this.storage.execute(() -> {
            ProviderStore.migrateLegacy(this);
            this.applyActiveModel();
            this.config = this.configStore.load();
            List<SessionRepository.Session> rows = this.repository.listSessions();
            this.cachedSessions = rows;
            String sid = rows.isEmpty() ? this.repository.createSession("\u65b0\u5bf9\u8bdd") : rows.get((int)0).id;
            List<ChatMessage> loaded = this.repository.getMessages(sid);
            this.runOnUiThread(() -> {
                this.sessionId = sid;
                this.messages.clear();
                this.messages.addAll(loaded);
                try {
                    this.showSessions();
                }
                catch (Throwable t) {
                    Log.e("Orbit", "build history", t);
                }
                try {
                    this.showChat();
                }
                catch (Throwable t) {
                    Log.e("Orbit", "build chat", t);
                }
                try {
                    this.prebuildSettings();
                }
                catch (Throwable t) {
                    Log.e("Orbit", "build settings", t);
                }
                this.snapToPage(1);
                this.chatVisible = true;
                this.historyVisible = false;
                this.refreshChatBody();
            });
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (this.handleCameraTouch(event)) {
            return true;
        }
        if (event.getAction() == 0) {
            this.downX = event.getX();
            this.downY = event.getY();
            this.suppressSwipe = this.chatVisible && (this.touches(this.reasoningControls, event) || this.touches(this.composer, event) || this.touches(this.sendButton, event) || AssistantMessageView.touchesTable(this.messageColumn, event.getRawX(), event.getRawY()));
            this.touchingChat = this.chatVisible && this.touches(this.scroll, event);
            this.chatTouchY = event.getY();
        } else if (event.getAction() == 2 && this.touchingChat) {
            if (event.getY() - this.chatTouchY > (float)ViewConfiguration.get(this).getScaledTouchSlop()) {
                this.followLatest = false;
                this.updateBottomButton();
            }
        } else if (event.getAction() == 3) {
            this.touchingChat = false;
        } else if (event.getAction() == 1) {
            this.touchingChat = false;
            this.followIfAllowed();
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.configStore != null) {
            this.scale = this.configStore.textScale();
            this.highContrast = this.configStore.highContrast();
            this.applyActiveModel();
            if (this.config == null) {
                this.config = this.loadConfig();
            }
            if (this.screen != null) {
                VisualEffects.applyAppearance(this, this.screen);
            }
            if (this.messageColumn != null && this.currentPageIndex == 1) {
                this.showChat();
            }
        }
    }

    private ChatConfig loadConfig() {
        try {
            return this.configStore.load();
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        this.startupHandler.removeCallbacksAndMessages(null);
        this.stop();
        ++this.requestSerial;
        this.storage.shutdown();
        super.onDestroy();
    }

    private int screenH() {
        int h;
        int n = h = this.screen != null && this.screen.getHeight() > 0 ? this.screen.getHeight() : 0;
        if (h <= 0) {
            h = this.getResources().getDisplayMetrics().heightPixels;
        }
        return h;
    }

    private int screenW() {
        int w;
        int n = w = this.pager != null && this.pager.getWidth() > 0 ? this.pager.getWidth() : 0;
        if (w <= 0) {
            w = this.getResources().getDisplayMetrics().widthPixels;
        }
        return w;
    }

    private void ensurePager() {
        if (this.pager != null) {
            return;
        }
        this.screen = new FrameLayout(this);
        this.pager = new FrameLayout(this);
        this.pager.setClipChildren(true);
        this.screen.addView((View)this.pager, new FrameLayout.LayoutParams(-1, -1));
        VisualEffects.install(this, this.screen);
    }

    private void putPage(int index, View page) {
        if (page == null) {
            return;
        }
        View old = this.pageSlots[index];
        if (old == page) {
            return;
        }
        if (old != null) {
            this.pager.removeView(old);
        }
        this.pageSlots[index] = page;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
        this.pager.addView(page, lp);
        VisualEffects.applyFontToTree(page, this);
        this.syncPages(0.0f);
    }

    private void applyCamera(float offset) {
        int w = this.screenW();
        for (int i = 0; i < 3; ++i) {
            int want;
            View page = this.pageSlots[i];
            if (page == null) continue;
            float pos = (float)(i - this.currentPageIndex) + offset;
            page.setTranslationX(pos * (float)w);
            int n = want = Math.abs(pos) < 1.0f ? 0 : 4;
            if (page.getVisibility() == want) continue;
            page.setVisibility(want);
        }
    }

    private void syncPages(float offset) {
        this.applyCamera(offset);
    }

    private View settingsPageView() {
        if (this.settingsPage == null) {
            if (this.settingsHost == null) {
                this.settingsHost = new SettingsPage.Host(){

                    @Override
                    public void onCloseSettings() {
                        MainActivity.this.goToPage(1, true);
                    }

                    @Override
                    public void onPickWallpaper() {
                        Intent pick = new Intent("android.intent.action.OPEN_DOCUMENT");
                        pick.addCategory("android.intent.category.OPENABLE");
                        pick.setType("image/*");
                        MainActivity.this.startActivityForResult(pick, 805);
                    }

                    @Override
                    public void onRequestTree(String treeUri) {
                        MainActivity.this.getSharedPreferences("file_features", 0).edit().putString("saf_tree", treeUri).apply();
                    }

                    @Override
                    public void onThemeChanged() {
                        // Colours are baked at construction, so the sibling pages carry the old
                        // mode until they are rebuilt. Rebuild them, then come back to settings.
                        //
                        // showSessions() sets currentPageIndex = 2 as a side effect, and putPage()
                        // only swaps a page's content — it never moves the camera. That is why the
                        // switch used to land the user on the history page. goToPage() is the only
                        // call that updates the index AND the camera together.
                        MainActivity.this.prebuildSettings();
                        MainActivity.this.showChat();
                        try { MainActivity.this.showSessions(); } catch (Throwable ignored) { }
                        MainActivity.this.putPage(0, MainActivity.this.settingsPageView());
                        MainActivity.this.goToPage(0, false);
                        VisualEffects.applyAppearance(MainActivity.this,
                                (android.view.View) MainActivity.this.getWindow()
                                        .getDecorView().getRootView());
                    }
                };
            }
            this.settingsPage = new SettingsPage(this, this.settingsHost);
        }
        return this.settingsPage;
    }

    private void prebuildSettings() {
        if (this.pager != null) {
            this.putPage(0, this.settingsPageView());
        }
    }

    private void goToPage(final int index, boolean animate) {
        if (this.pager == null) {
            return;
        }
        if (index == 0) {
            this.settingsPageView();
        }
        int from = this.currentPageIndex;
        if (!animate) {
            this.currentPageIndex = index;
            this.syncPages(0.0f);
            this.afterPageEnter(index);
            return;
        }
        int w = this.screenW();
        ValueAnimator va = ValueAnimator.ofFloat(from, index);
        va.setDuration(260L);
        va.setInterpolator(VisualEffects.EASE_IN_OUT);
        va.addUpdateListener(a -> {
            float v = ((Float)a.getAnimatedValue()).floatValue();
            for (int i = 0; i < 3; ++i) {
                View page = this.pageSlots[i];
                if (page == null) continue;
                page.setTranslationX(((float)i - v) * (float)w);
            }
        });
        va.addListener(new AnimatorListenerAdapter(){

            @Override
            public void onAnimationEnd(Animator a) {
                MainActivity.this.currentPageIndex = index;
                MainActivity.this.syncPages(0.0f);
                MainActivity.this.afterPageEnter(index);
            }
        });
        va.start();
    }

    private void afterPageEnter(int index) {
        this.chatVisible = index == 1;
        boolean bl = this.historyVisible = index == 2;
        if (index == 1) {
            if (this.messageColumn != null && this.messageColumn.getChildCount() == 0) {
                this.welcome();
            }
            this.rebuildReasoningRow();
        }
    }

    private void snapToPage(int index) {
        this.goToPage(index, false);
    }

    private int camSlop() {
        if (this.camSlop <= 0) {
            this.camSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        }
        return this.camSlop;
    }

    private boolean pointInsideInteractive(MotionEvent e) {
        ArrayList<View> owned = new ArrayList<View>();
        if (this.reasoningSlider != null) {
            owned.add(this.reasoningSlider);
        }
        if (this.settingsPage != null) {
            owned.addAll(this.settingsPage.horizontalControls());
        }
        for (View v : owned) {
            if (v == null || v.getVisibility() != 0) continue;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            if (!(e.getRawX() >= (float)pos[0]) || !(e.getRawX() < (float)(pos[0] + v.getWidth())) || !(e.getRawY() >= (float)pos[1]) || !(e.getRawY() < (float)(pos[1] + v.getHeight()))) continue;
            return true;
        }
        return false;
    }

    private boolean handleCameraTouch(MotionEvent e) {
        int w = this.screenW();
        if (w <= 0) {
            return false;
        }
        switch (e.getActionMasked()) {
            case 0: {
                this.camDownX = e.getX();
                this.camDownY = e.getY();
                this.camLastX = this.camDownX;
                this.camLastT = e.getEventTime();
                this.camTracking = true;
                this.camDecided = false;
                this.camApplied = false;
                this.camYieldsToChild = this.pointInsideInteractive(e);
                if (this.velocityTracker == null) {
                    this.velocityTracker = VelocityTracker.obtain();
                }
                this.velocityTracker.clear();
                this.velocityTracker.addMovement(e);
                View focused = this.getCurrentFocus();
                if (focused instanceof EditText) {
                    ((EditText)focused).clearFocus();
                }
                return false;
            }
            case 2: {
                if (!this.camTracking) {
                    return false;
                }
                if (this.velocityTracker != null) {
                    this.velocityTracker.addMovement(e);
                }
                float dx = e.getX() - this.camDownX;
                float dy = e.getY() - this.camDownY;
                if (this.camYieldsToChild) {
                    this.camTracking = false;
                    return false;
                }
                if (!this.camDecided) {
                    if (dx * dx + dy * dy <= (float)(this.camSlop() * this.camSlop())) {
                        return false;
                    }
                    if (Math.abs(dy) >= Math.abs(dx)) {
                        this.camTracking = false;
                        return false;
                    }
                    this.camDecided = true;
                }
                if (!this.camDecided) {
                    return false;
                }
                this.camApplied = true;
                this.applyCamera(this.clampCameraOffset(dx / (float)w));
                return true;
            }
            case 1: {
                boolean far;
                boolean flung;
                if (!this.camTracking) {
                    this.camTracking = false;
                    this.recycleVelocity();
                    return false;
                }
                this.camTracking = false;
                float dx = e.getX() - this.camDownX;
                float off = this.clampCameraOffset(dx / (float)w);
                float velocityX = 0.0f;
                if (this.velocityTracker != null) {
                    this.velocityTracker.addMovement(e);
                    this.velocityTracker.computeCurrentVelocity(1000);
                    velocityX = this.velocityTracker.getXVelocity();
                }
                this.recycleVelocity();
                float fling = this.dp(300.0f);
                boolean bl = flung = Math.abs(velocityX) > fling && Math.abs(dx) > (float)this.dp(18.0f);
                if (!flung && Math.abs(dx) < (float)this.camSlop() && Math.abs(off) < 0.02f) {
                    this.applyCamera(0.0f);
                    return false;
                }
                boolean bl2 = far = Math.abs(off) > 0.28f;
                if (flung || far) {
                    int dir = flung ? (velocityX < 0.0f ? 1 : -1) : (off < 0.0f ? 1 : -1);
                    int target = Math.max(0, Math.min(2, this.currentPageIndex + dir));
                    if (target != this.currentPageIndex) {
                        this.commitCamera(target);
                        return true;
                    }
                }
                this.returnSpring();
                return true;
            }
            case 3: {
                this.recycleVelocity();
                if (this.camTracking) {
                    this.camTracking = false;
                    this.returnSpring();
                    return this.camApplied;
                }
                return false;
            }
        }
        return false;
    }

    private void recycleVelocity() {
        if (this.velocityTracker != null) {
            this.velocityTracker.recycle();
            this.velocityTracker = null;
        }
    }

    private float clampCameraOffset(float off) {
        if (this.currentPageIndex == 0) {
            off = Math.min(0.0f, off);
        }
        if (this.currentPageIndex == 2) {
            off = Math.max(0.0f, off);
        }
        return Math.max(-1.0f, Math.min(1.0f, off));
    }

    private void commitCamera(final int target) {
        int w = this.screenW();
        float from = this.getPageTranslationOffset();
        ValueAnimator va = ValueAnimator.ofFloat(from, this.currentPageIndex - target);
        va.setDuration(240L);
        va.setInterpolator(VisualEffects.EASE_IN_OUT);
        va.addUpdateListener(a -> this.applyCamera(((Float)a.getAnimatedValue()).floatValue()));
        va.addListener(new AnimatorListenerAdapter(){

            @Override
            public void onAnimationEnd(Animator a) {
                MainActivity.this.currentPageIndex = target;
                MainActivity.this.syncPages(0.0f);
                MainActivity.this.afterPageEnter(target);
            }
        });
        va.start();
    }

    private float getPageTranslationOffset() {
        View cur = this.pageSlots[this.currentPageIndex];
        if (cur == null) {
            return 0.0f;
        }
        int w = this.screenW();
        return w <= 0 ? 0.0f : cur.getTranslationX() / (float)w;
    }

    private void returnSpring() {
        int w = this.screenW();
        float from = this.getPageTranslationOffset();
        if (Math.abs(from) < 0.001f) {
            this.syncPages(0.0f);
            return;
        }
        ValueAnimator va = ValueAnimator.ofFloat(from, 0.0f);
        va.setDuration(240L);
        va.setInterpolator(new OvershootInterpolator(1.1f));
        va.addUpdateListener(a -> this.applyCamera(((Float)a.getAnimatedValue()).floatValue()));
        va.start();
    }

    private void refreshChatBody() {
        this.welcomeView = null;
        if (this.messageColumn == null) {
            return;
        }
        this.render();
        if (this.messageColumn.getChildCount() == 0) {
            this.welcome();
        }
    }

    private void showLoading() {
        this.ensurePager();
        this.root = this.col();
        this.root.setGravity(17);
        this.root.setPadding(0, 0, 0, 0);
        this.root.setBackgroundColor(-16777216);
        this.root.setClickable(true);
        int acc = AppearancePreferences.accent(this);
        LinearLayout overlay = this.root;
        SplashView splash = new SplashView((Context)this, acc, () -> this.dismissSplash(overlay));
        this.splash = splash;
        splash.setLiftTarget((float)this.screenH() * 0.3f);
        this.root.addView((View)splash, new LinearLayout.LayoutParams(-1, -1));
        this.splashOverlay = overlay;
        this.screen.addView((View)overlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void dismissSplash(View overlay) {
        if (overlay == null || this.isFinishing() || this.isDestroyed()) return;
        this.chatVisible = true;
        this.historyVisible = false;
        this.snapToPage(1);

        // The mark travels rather than dissolves: the splash is a full-screen overlay, so
        // instead of fading it out we scale it down to the header slot and slide it there.
        // The glyph the boot sequence assembled is then the same glyph that stays as the title.
        // Boot does not fly any more: the mark's resting place after the sequence is the
        // middle of the empty conversation, so the overlay simply goes away and the centre
        // copy is already there underneath it.
        overlay.animate().alpha(0f).setDuration(300L)
                .setInterpolator(VisualEffects.EASE_OUT)
                .withEndAction(() -> {
                    if (overlay.getParent() instanceof ViewGroup) {
                        ((ViewGroup) overlay.getParent()).removeView(overlay);
                    }
                    if (this.splashOverlay == overlay) this.splashOverlay = null;
                    this.followIfAllowed();
                }).start();
        return;

    }

    /**
     * Moves the finished boot mark into the header slot.
     *
     * Geometry is read from both views rather than assumed: the header slot's position depends
     * on the status row, and the mark sits above centre inside the splash. The splash keeps
     * drawing its settled frame for the whole flight, so no frame is ever blank.
     */
    private void flySplashToHeader(View overlay, SplashView splash) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
            if (overlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) overlay.getParent()).removeView(overlay);
            }
            if (this.splashOverlay == overlay) this.splashOverlay = null;
            this.followIfAllowed();
            return;
        }

        splash.stopClock();
        this.headerLogo.setAlpha(0f);
        final int[] targetLoc = new int[2];
        final int[] targetSize = new int[2];

        this.headerLogo.post(() -> {
            this.headerLogo.getLocationInWindow(targetLoc);
            targetSize[0] = this.headerLogo.getWidth();

            int[] sl = new int[2];
            splash.getLocationInWindow(sl);
            final float markLeft = sl[0] + (splash.getWidth() - splash.markWidth()) / 2f;
            final float markTop = sl[1] + splash.markTop();
            final float markW = Math.max(1f, splash.markWidth());
            final float pivotX = markLeft + markW / 2f - sl[0];
            final float pivotY = markTop + splash.markHeight() / 2f - sl[1];

            final float dx = targetLoc[0] - markLeft;
            final float dy = targetLoc[1] - markTop;
            final float scale = targetSize[0] / markW;

            // Driven inside the draw pass: a translation set through ViewPropertyAnimator is
            // cleared as soon as the parent relays out, which is exactly what happens while the
            // chat page finishes building.
            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, 1f);
            va.setDuration(420L);
            va.setInterpolator(VisualEffects.EASE_IN_OUT);
            va.addUpdateListener(a -> {
                float k = (float) a.getAnimatedValue();
                // Keep full opacity for most of the flight: the mark landing should be seen.
                // It only fades in the last third, where the header logo takes over.
                float alpha = k < 0.72f ? 1f : 1f - ((k - 0.72f) / 0.28f);
                splash.setFlight(dx * k, dy * k,
                        1f + (scale - 1f) * k,
                        alpha,
                        pivotX, pivotY);
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    // overlay was captured before showChat() reassigned root, so it is the boot
                    // layer, not the chat page. Removing it takes the splash with it.
                    if (overlay.getParent() instanceof ViewGroup) {
                        ((ViewGroup) overlay.getParent()).removeView(overlay);
                    }
                    if (splash.getParent() instanceof ViewGroup) {
                        ((ViewGroup) splash.getParent()).removeView(splash);
                    }
                    if (splashOverlay == overlay) splashOverlay = null;
                    if (headerLogo != null) {
                        headerLogo.animate().alpha(1f).setDuration(160L)
                                .setInterpolator(VisualEffects.EASE_OUT).start();
                    }
                    followIfAllowed();
                }
            });
            va.start();
        });
    }

    private void header(String title, String action, Runnable click) {
        this.historyVisible = false;
        this.rememberReading();
        this.chatVisible = false;
        this.root = this.col();
        this.root.setBackgroundColor(-16777216);
        this.root.setPadding(this.dp(12.0f), this.dp(5.0f), this.dp(12.0f), this.dp(6.0f));
        VisualEffects.applyAppearance(this, this.root);
        this.putPage(1, this.root);
        LinearLayout bar = this.row();
        TextView back = this.text("\u2039", 32, this.fg());
        back.setGravity(17);
        bar.addView((View)back, new LinearLayout.LayoutParams(this.dp(52.0f), this.dp(54.0f)));
        back.setOnClickListener(v -> this.showChat());
        TextView heading = this.text(title, this.uiSize() + 2, this.fg());
        heading.setTypeface(Typeface.DEFAULT, 1);
        bar.addView((View)heading, new LinearLayout.LayoutParams(0, this.dp(54.0f), 1.0f));
        TextView act = this.text(action, this.uiSize(), this.accent());
        act.setGravity(17);
        bar.addView((View)act, new LinearLayout.LayoutParams(this.dp(60.0f), this.dp(54.0f)));
        act.setOnClickListener(v -> click.run());
        this.root.addView(bar);
        View line = new View(this);
        line.setBackgroundColor(AppearancePreferences.lightMode(this)
                ? 0xffdcdcd8 : Color.rgb(49, 52, 47));
        this.root.addView(line, new LinearLayout.LayoutParams(-1, this.dp(1.0f)));
    }

    private void showChat() {
        boolean same;
        this.pageFrom = this.historyVisible ? 1.0f : 0.0f;
        this.historyVisible = false;
        this.rememberReading();
        boolean bl = same = this.sessionId != null && this.sessionId.equals(this.displayedSession);
        if (!same) {
            this.followLatest = true;
            this.readingY = 0;
        }
        this.displayedSession = this.sessionId;
        this.chatVisible = true;
        this.streamBody = null;
        this.thinkingBody = null;
        this.thinkingWrap = null;
        this.root = this.col();
        this.root.setBackgroundColor(-16777216);
        this.root.setPadding(this.dp(16.0f), this.dp(8.0f), this.dp(16.0f), this.dp(12.0f));
        VisualEffects.applyAppearance(this, this.root);
        this.currentPageIndex = 1;
        this.chatVisible = true;
        this.historyVisible = false;
        this.putPage(1, this.root);
        if (this.cachedSessions.isEmpty()) {
            this.storage.execute(() -> {
                List<SessionRepository.Session> rows = this.repository.listSessions();
                this.cachedSessions = rows;
            });
        }
        if (this.pageFrom != 0.0f) {
            VisualEffects.pageTransition(this.root, 40.0f * this.getResources().getDisplayMetrics().density * this.pageFrom);
            this.pageFrom = 0.0f;
        }
        // The header shows the pixel mark, not a word: the boot sequence settles this same
        // glyph in the middle of the screen and it should end up here rather than vanish.
        // Transient strings (connecting, search) still use activityStatus, drawn over the mark
        // only while it has something to say.
        this.activityStatus = this.text("", this.uiSize(), this.muted());
        this.activityStatus.setGravity(17);
        LinearLayout statusRow = this.row();
        statusRow.setGravity(17);

        // Left slot: the loading ring, or an empty view of the same width so the title stays
        // optically centred whether or not a request is running.
        FrameLayout leftSlot = new FrameLayout(this);
        this.loadingIndicator = new VisualEffects.LoadingRing(this);
        this.loadingIndicator.setVisibility(this.generating ? 0 : 8);
        FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(this.dp(28.0f), this.dp(28.0f), 17);
        leftSlot.addView(this.loadingIndicator, ringLp);
        statusRow.addView(leftSlot, new LinearLayout.LayoutParams(this.dp(44.0f), this.dp(38.0f)));

        // A FrameLayout centre slot: the logo sits still and the status string draws
        // over it only while there is one, so the header never reflows between the two.
        FrameLayout centreSlot = new FrameLayout(this);
        this.headerLogo = new SplashView(this, AppearancePreferences.accent(this), null);
        this.headerLogo.setTitleMode(true);
        this.headerLogo.setAlpha(0f);   // revealed only when the centre mark flies up
        this.headerLogo.setBackgroundColor(Color.TRANSPARENT);
        centreSlot.addView(this.headerLogo,
                new FrameLayout.LayoutParams(this.dp(104.0f), this.dp(20.0f), 17));

        this.activityStatus.setGravity(17);
        this.activityStatus.setSingleLine(true);
        this.activityStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        centreSlot.addView(this.activityStatus,
                new FrameLayout.LayoutParams(-1, this.dp(38.0f), 17));
        // The status string and the mark share one slot. Showing both painted text straight
        // over the glyphs, so a non-empty status now suppresses the mark for its lifetime.
        this.activityStatus.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable e) {
                if (headerLogo == null) return;
                headerLogo.setAlpha(e != null && e.length() > 0 ? 0f : 1f);
            }
        });
        statusRow.addView(centreSlot, new LinearLayout.LayoutParams(0, this.dp(38.0f), 1.0f));

        TextView detail = this.text("⋯", 20, this.muted());
        detail.setGravity(17);
        detail.setContentDescription("当前对话信息");
        statusRow.addView((View)detail, new LinearLayout.LayoutParams(this.dp(40.0f), this.dp(38.0f)));
        pressFeedback(detail);
        detail.setOnClickListener(v -> this.openCurrentSessionInfo());

        this.root.addView((View)statusRow, new LinearLayout.LayoutParams(-1, -2));
        this.chatArea = chatArea = new FrameLayout(this);
        this.scroll = new ScrollView(this){

            @Override
            public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
                return MainActivity.this.followLatest && super.requestChildRectangleOnScreen(child, rect, immediate);
            }
        };
        this.scroll.setFillViewport(true);
        this.messageColumn = this.col();
        this.messageColumn.setPadding(this.dp(2.0f), this.dp(4.0f), this.dp(2.0f), this.dp(8.0f));
        this.scroll.addView(this.messageColumn);
        chatArea.addView((View)this.scroll, new FrameLayout.LayoutParams(-1, -1));
        TextView bottom = this.text("\u2193", 22, this.accent());
        bottom.setGravity(17);
        bottom.setBackground(this.shape(this.panel(), 24));
        bottom.setContentDescription("\u56de\u5230\u6700\u65b0\u6d88\u606f");
        FrameLayout.LayoutParams bpDown = new FrameLayout.LayoutParams(this.dp(48.0f), this.dp(48.0f), 83);
        bpDown.setMargins(this.dp(8.0f), 0, 0, this.dp(10.0f));
        chatArea.addView((View)bottom, bpDown);
        bottom.setVisibility(8);
        this.bottomButton = bottom;
        bottom.setOnClickListener(v -> {
            this.followLatest = true;
            this.touchingChat = false;
            this.followIfAllowed();
        });
        this.scroll.setOnScrollChangeListener((v, x, y, ox, oy) -> {
            if (this.touchingChat && y < oy) {
                this.followLatest = false;
            }
            this.readingY = y;
            this.updateBottomButton();
        });
        // A FrameLayout wrapper lets the panel float above the transcript: the chat area
        // takes the space, the overlay sits on top and never consumes layout height.
        FrameLayout chatStack = new FrameLayout(this);
        chatStack.addView((View)chatArea, new FrameLayout.LayoutParams(-1, -1));
        this.reasoningOverlay = new FrameLayout(this);
        this.reasoningOverlay.setClickable(false);
        FrameLayout.LayoutParams hostLp = new FrameLayout.LayoutParams(-1, -2);
        hostLp.gravity = Gravity.BOTTOM;
        hostLp.setMargins(this.dp(8.0f), 0, this.dp(8.0f), this.dp(6.0f));
        chatStack.addView((View)this.reasoningOverlay, hostLp);
        this.chatChrome = chatStack;   // the whole chat surface: what the splash hands off to
        this.root.addView((View)chatStack, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        if (this.messages.isEmpty()) {
            // Empty conversation: the mark belongs in the middle, header stays bare.
            this.welcome();
            if (this.headerLogo != null) this.headerLogo.setAlpha(0f);
        } else {
            // Conversation already has content, so the mark has no middle to occupy — it is
            // already at home in the header. Without this the header stayed blank forever on
            // any reopened conversation, because nothing else ever raises its alpha.
            this.render();
            if (this.headerLogo != null) this.headerLogo.setAlpha(1f);
        }
        LinearLayout box = this.col();
        box.setPadding(this.dp(12.0f), this.dp(9.0f), this.dp(12.0f), this.dp(9.0f));
        VisualEffects.raisedSurface(box, false);
        LinearLayout input = this.row();
        this.composer = new EditText(this);
        this.composer.setText(this.composerDraft);
        this.composer.setTextColor(this.fg());
        this.composer.setHintTextColor(this.muted());
        this.composer.setHint("");
        this.composer.setTextSize(this.bodySize());
        this.composer.setMinHeight(this.dp(50.0f));
        this.composer.setMaxLines(3);
        this.composer.setPadding(this.dp(8.0f), this.dp(5.0f), this.dp(6.0f), this.dp(5.0f));
        this.composer.setBackgroundColor(0);
        this.composer.setInputType(147457);
        this.composer.setImeOptions(4);
        input.addView((View)this.composer, new LinearLayout.LayoutParams(0, -2, 1.0f));
        TextView attach = this.text("\uff0b", 24, WHITE);
        attach.setGravity(17);
        attach.setContentDescription("\u5bfc\u5165\u6587\u4ef6");
        VisualEffects.roundKey(attach, false);
        LinearLayout.LayoutParams attachParams = new LinearLayout.LayoutParams(this.dp(52.0f), this.dp(52.0f));
        attachParams.leftMargin = this.dp(4.0f);
        attachParams.rightMargin = this.dp(4.0f);
        input.addView((View)attach, attachParams);
        attach.setOnClickListener(v -> this.showImportChooser());
        SendButton send = new SendButton(this);
        send.setText(this.generating ? "\u25a0" : "\u2191");
        this.sendButton = send;
        send.setGravity(17);
        send.setContentDescription(this.generating ? "\u505c\u6b62\u751f\u6210" : "\u53d1\u9001\u6d88\u606f");
        VisualEffects.roundKey(send, true);
        input.addView((View)send, new LinearLayout.LayoutParams(this.dp(52.0f), this.dp(52.0f)));
        send.setOnClickListener(v -> {
            if (this.generating) {
                this.stop();
            } else {
                this.send();
            }
        });
        this.addReasoningControls(this.root);
        box.addView(input);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = this.dp(5.0f);
        this.root.addView((View)box, bp);
        if (!this.imageAttachments.isEmpty()) {
            LinearLayout chipRow = this.row();
            for (Map.Entry<String, Attachment> e2 : this.imageAttachments.entrySet()) {
                String key = e2.getKey();
                LinearLayout chip = this.row();
                GradientDrawable chipBg = new GradientDrawable();
                chipBg.setColor(AppearancePreferences.panel(this));
                chipBg.setCornerRadius(this.dp(18.0f));
                chipBg.setStroke(this.dp(1.0f), AppearancePreferences.mutedAccent(this));
                chip.setBackground(chipBg);
                chip.setPadding(this.dp(10.0f), this.dp(4.0f), this.dp(10.0f), this.dp(4.0f));
                TextView icon = this.text("\ud83d\uddbc", 14, this.accent());
                chip.addView(icon);
                TextView label = this.text(e2.getValue().displayName, this.uiSize() - 2, this.muted());
                label.setMaxLines(1);
                chip.addView((View)label, new LinearLayout.LayoutParams(0, -2, 1.0f));
                TextView close = this.text("\u00d7", 16, this.muted());
                close.setPadding(this.dp(6.0f), 0, this.dp(2.0f), 0);
                close.setContentDescription("\u79fb\u9664\u56fe\u7247\u9644\u4ef6");
                close.setOnClickListener(v -> {
                    this.imageAttachments.remove(key);
                    this.showChat();
                });
                chip.addView(close);
                chipRow.addView((View)chip, new LinearLayout.LayoutParams(-2, -2));
            }
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-1, -2);
            chipParams.topMargin = this.dp(4.0f);
            chipParams.bottomMargin = this.dp(6.0f);
            this.root.addView((View)chipRow, chipParams);
        }
        this.composer.setOnEditorActionListener((v, a, e) -> {
            if (a == 4) {
                this.send();
                return true;
            }
            return false;
        });
        if (this.generating && this.sessionId.equals(this.requestTarget)) {
            if (!this.thoughtRaw.isEmpty()) {
                this.updateThinking(this.thoughtRaw);
            }
            if (!this.answerRaw.isEmpty()) {
                this.updateStream(this.answerRaw);
            }
        }
        ScrollView pane = this.scroll;
        int restoreY = this.readingY;
        pane.post(() -> {
            if (pane == this.scroll && this.chatVisible && !this.followLatest) {
                pane.scrollTo(0, restoreY);
            }
        });
        VisualEffects.applyFontToTree(this.root, this);
        this.followIfAllowed();
    }

    private void openSettings() {
        this.rememberReading();
        this.chatVisible = false;
        this.historyVisible = false;
        this.goToPage(0, true);
    }

    private void showSettings() {
        this.openSettings();
    }

    private void welcome() {
        // The mark rests HERE while the conversation is empty. It only travels to the header
        // once a message needs the space — the header copy stays invisible until then.
        LinearLayout center = this.col();
        center.setGravity(17);
        center.setAlpha(1f);

        this.centerMark = new SplashView(this, AppearancePreferences.accent(this), null);
        this.centerMark.setTitleMode(true);
        this.centerMark.setBackgroundColor(Color.TRANSPARENT);
        centreMarkLp = new LinearLayout.LayoutParams(this.dp(180.0f), this.dp(30.0f));
        center.addView(this.centerMark, centreMarkLp);

        this.messageColumn.addView((View)center, new LinearLayout.LayoutParams(-1, -1));
        this.welcomeView = center;
    }


    /**
     * Sends the resting mark from the middle of an empty conversation up to the header.
     *
     * This fires on the first message, not at boot: while the conversation is empty the mark
     * belongs in the middle, and it is only the arrival of content that takes away the room for
     * it. Once it has landed the centre copy is detached so the two never coexist.
     */
    /**
     * Carries the resting mark into the header, once the first message takes its space.
     *
     * The centre view cannot be animated directly: showChat() rebuilds the message column and
     * detaches it mid-flight. So its position is snapshotted first, an overlay copy is placed
     * exactly there, that copy flies, and the header logo crossfades in where it lands.
     */
    private void flyMarkToHeader(int[] fromLoc, int[] fromSize) {
        if (this.screen == null) return;

        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
            if (this.headerLogo != null) this.headerLogo.setAlpha(1f);
            return;
        }

        if (this.headerLogo == null) return;
        this.headerLogo.setAlpha(0f);
        final SplashView ghost = new SplashView(this, AppearancePreferences.accent(this), null);
        ghost.setTitleMode(true);
        ghost.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(
                Math.max(1, fromSize[0]), Math.max(1, fromSize[1]));
        glp.leftMargin = fromLoc[0];
        glp.topMargin = fromLoc[1] - this.screenTop();
        this.screen.addView(ghost, glp);

        final SplashView target = this.headerLogo;
        target.post(() -> {
            // Both locations are window coordinates, and the ghost is positioned in the same
            // space, so the delta is a plain subtraction. Mixing host offsets in here produced
            // a double-corrected value and the mark flew off-screen.
            int[] toLoc = new int[2];
            target.getLocationInWindow(toLoc);
            final float dx = toLoc[0] - fromLoc[0];
            final float dy = toLoc[1] - fromLoc[1];
            final float scale = target.getWidth() / Math.max(1f, (float) fromSize[0]);
            final float pivotX = fromSize[0] / 2f;
            final float pivotY = fromSize[1] / 2f;

            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, 1f);
            va.setDuration(420L);
            va.setInterpolator(VisualEffects.EASE_IN_OUT);
            va.addUpdateListener(a -> {
                float k = (float) a.getAnimatedValue();
                float alpha = k < 0.72f ? 1f : 1f - ((k - 0.72f) / 0.28f);
                ghost.setFlight(dx * k, dy * k, 1f + (scale - 1f) * k, alpha, pivotX, pivotY);
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    if (ghost.getParent() instanceof ViewGroup) {
                        ((ViewGroup) ghost.getParent()).removeView(ghost);
                    }
                    // Re-read the field: showChat() may have swapped the instance again.
                    if (headerLogo != null) {
                        headerLogo.setAlpha(1f);
                    }
                }
            });
            va.start();
        });
    }

    private int screenTop() {
        if (this.screen == null) return 0;
        int[] loc = new int[2];
        this.screen.getLocationInWindow(loc);
        return loc[1];
    }

    private void fadeInWelcome() {
        if (this.welcomeView == null || this.welcomeView.getParent() == null) {
            return;
        }
        this.welcomeView.setAlpha(0.0f);
        if (VisualEffects.reducedMotion(this.welcomeView)) {
            this.welcomeView.setAlpha(1.0f);
            return;
        }
        this.welcomeView.animate().alpha(1.0f).setStartDelay(60L).setDuration(240L).setInterpolator(VisualEffects.EASE_OUT).start();
    }

    private void render() {
        this.messageColumn.removeAllViews();
        for (ChatMessage m : this.messages) {
            View body;
            String thought;
            if (m.role == ChatMessage.Role.ASSISTANT && (thought = this.reasoningByMessage.get(m.id)) != null && !thought.isEmpty()) {
                this.addThinkingView(thought);
            }
            boolean user = m.role == ChatMessage.Role.USER;
            LinearLayout bubble = this.col();
            bubble.setPadding(this.dp(13.0f), this.dp(11.0f), this.dp(13.0f), this.dp(11.0f));
            if (user) {
                bubble.setBackground(this.shape(this.panel(), 15));
            }
            TextView who = this.text(user ? "\u6211" : "\u58a8\u8bed", 12, user ? this.accent() : this.muted());
            who.setTypeface(Typeface.DEFAULT, 1);
            bubble.addView(who);
            if (m.role == ChatMessage.Role.ASSISTANT) {
                body = new AssistantMessageView((Context)this, this.bodySize(), this.fg());
                ((AssistantMessageView)body).setRaw(m.content);
                bubble.addView(body, new LinearLayout.LayoutParams(-1, -2));
            } else {
                body = this.text(m.content, this.bodySize(), this.fg());
                ((TextView)body).setGravity(48);
                ((TextView)body).setLineSpacing(this.dp(2.0f), 1.05f);
                ((TextView)body).setTextIsSelectable(false);
                bubble.addView(body);
            }
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(user ? -2 : -1, -2);
            p.gravity = user ? 5 : 3;
            p.leftMargin = this.dp(user ? 26.0f : 0.0f);
            p.rightMargin = this.dp(user ? 0.0f : 12.0f);
            p.bottomMargin = this.dp(12.0f);
            this.messageColumn.addView((View)bubble, p);
        }
        this.followIfAllowed();
    }

    private void send() {
        String target;
        String value = this.composer.getText().toString().trim();
        ArrayList<Attachment> sendingImages = new ArrayList<Attachment>(this.imageAttachments.values());
        if (value.isEmpty() && sendingImages.isEmpty() || this.generating) {
            return;
        }
        this.applyActiveModel();
        if (ProviderStore.activeProvider(this) == null) {
            this.config = null;
        }
        if (this.config == null || this.config.apiKey.trim().isEmpty()) {
            this.showSettings();
            Hints.show(this, "\u8bf7\u5148\u5728\u8bbe\u7f6e\u4e2d\u6dfb\u52a0\u4f9b\u5e94\u5546\u5e76\u586b\u5199 Key");
            return;
        }
        RequestOptions requestOptions = new RequestOptions(false, "", "", this.selectedReasoning(), this.selectedMaxOutput());
        ChatMessage user = ChatMessage.of(ChatMessage.Role.USER, value);
        // Capture the resting mark's geometry BEFORE render() tears the column down, then fly a
        // fresh overlay copy from that spot. The live view cannot be reused because render()
        // detaches it mid-flight.
        final int[] markLoc = new int[2];
        final int[] markSize = new int[2];
        final boolean hadCentreMark = this.centerMark != null
                && this.centerMark.getParent() != null;
        if (hadCentreMark) {
            this.centerMark.getLocationInWindow(markLoc);
            markSize[0] = this.centerMark.getWidth();
            markSize[1] = this.centerMark.getHeight();
        }
        this.messages.add(user);
        this.composer.setText("");
        this.imageAttachments.clear();
        this.generating = true;
        long serial = ++this.requestSerial;
        this.requestTarget = target = this.sessionId;
        this.answerRaw = "";
        this.thoughtRaw = "";
        this.searchNotice = "";
        this.reasoningExpanded = false;
        this.followLatest = true;
        this.pendingSaveContent = "";
        this.pendingImageNames.clear();
        this.pendingImageNames.addAll(this.imageAttachments.keySet());
        this.showChat();

        // After the rebuild, not before: showChat() detaches the resting mark.
        if (hadCentreMark) {
            this.flyMarkToHeader(markLoc, markSize);
        }
        ChatConfig requestConfig = this.config;
        this.storage.execute(() -> {
            String requestKey = this.repository.beginRequest(target);
            this.repository.appendMessage(target, user);
            List<ChatMessage> full = this.repository.getMessages(target);
            List<ChatMessage> history = this.trimHistory(full);
            this.runOnUiThread(() -> {
                if (this.ownsRequest(serial)) {
                    this.beginRequest(serial, target, requestKey, history, requestConfig, requestOptions, sendingImages);
                }
            });
        });
    }

    /** One-line session title with a visible ellipsis when it does not fit. */
    private TextView sessionTitle(String title){
        TextView name = this.text(title, this.uiSize(), this.fg());
        name.setSingleLine(true);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(this.dp(10.0f), 0, this.dp(6.0f), 0);
        return name;
    }

    private void renderSessionList(LinearLayout items, List<SessionRepository.Session> rows) {
        items.removeAllViews();
        for (SessionRepository.Session session : rows) {
            LinearLayout line = this.row();
            TextView name = this.sessionTitle(session.title);
            line.addView((View)name, new LinearLayout.LayoutParams(0, this.dp(58.0f), 1.0f));
            TextView info = this.text("\u24d8", this.uiSize() + 2, this.accent());
            info.setGravity(17);
            info.setContentDescription("\u5bf9\u8bdd\u4fe1\u606f");
            line.addView((View)info, new LinearLayout.LayoutParams(this.dp(48.0f), this.dp(56.0f)));
            info.setOnClickListener(v -> this.showSessionInfo(session));
            TextView del = this.text("\u5220\u9664", this.uiSize() - 1, this.muted());
            del.setGravity(17);
            line.addView((View)del, new LinearLayout.LayoutParams(this.dp(68.0f), this.dp(56.0f)));
            del.setOnClickListener(v -> this.confirmDelete(() -> this.deleteSession(session.id)));
            name.setOnClickListener(v -> this.loadSession(session.id));
            items.addView(line);
            View divider = new View(this);
            divider.setBackgroundColor(AppearancePreferences.mutedAccent(this));
            items.addView(divider, new LinearLayout.LayoutParams(-1, this.dp(1.0f)));
        }
        if (rows.isEmpty()) {
            items.addView((View)this.text("\u8fd8\u6ca1\u6709\u5bf9\u8bdd\u8bb0\u5f55", this.uiSize(), this.muted()), new LinearLayout.LayoutParams(-1, this.dp(70.0f)));
        }
    }

    private void showSessions() {
        this.historyVisible = true;
        this.rememberReading();
        this.chatVisible = false;
        this.root = this.col();
        this.root.setBackgroundColor(-16777216);
        this.root.setPadding(this.dp(16.0f), this.dp(8.0f), this.dp(16.0f), this.dp(12.0f));
        VisualEffects.applyAppearance(this, this.root);
        this.currentPageIndex = 2;
        this.chatVisible = false;
        this.historyVisible = true;
        this.putPage(2, this.root);
        if (this.cachedSessions.isEmpty()) {
            this.storage.execute(() -> {
                List<SessionRepository.Session> rows = this.repository.listSessions();
                this.cachedSessions = rows;
            });
        }
        if (this.pageFrom != 0.0f) {
            VisualEffects.pageTransition(this.root, 40.0f * this.getResources().getDisplayMetrics().density * this.pageFrom);
            this.pageFrom = 0.0f;
        }
        LinearLayout top = this.row();
        TextView title = this.text("\u5386\u53f2\u5bf9\u8bdd", this.uiSize() + 2, this.fg());
        title.setTypeface(VisualEffects.type(this));
        top.addView((View)title, new LinearLayout.LayoutParams(0, this.dp(48.0f), 1.0f));
        TextView add = this.text("\uff0b", 28, this.accent());
        add.setGravity(17);
        add.setContentDescription("\u65b0\u5efa\u5bf9\u8bdd");
        top.addView((View)add, new LinearLayout.LayoutParams(this.dp(56.0f), this.dp(52.0f)));
        add.setOnClickListener(v -> this.newSession());
        this.root.addView(top);
        ScrollView area = new ScrollView(this);
        LinearLayout items = this.col();
        area.addView(items);
        this.root.addView((View)area, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        if (!this.cachedSessions.isEmpty()) {
            this.renderSessionList(items, this.cachedSessions);
        }
        VisualEffects.applyFontToTree(this.root, this);
        this.storage.execute(() -> {
            List<SessionRepository.Session> rows = this.repository.listSessions();
            this.cachedSessions = rows;
            this.runOnUiThread(() -> {
                items.removeAllViews();
                for (SessionRepository.Session session : rows) {
                    LinearLayout line = this.row();
                    TextView name = this.sessionTitle(session.title);
                    line.addView((View)name, new LinearLayout.LayoutParams(0, this.dp(58.0f), 1.0f));
                    TextView info = this.text("\u24d8", this.uiSize() + 2, this.accent());
                    info.setGravity(17);
                    info.setContentDescription("\u5bf9\u8bdd\u4fe1\u606f");
                    line.addView((View)info, new LinearLayout.LayoutParams(this.dp(48.0f), this.dp(56.0f)));
                    info.setOnClickListener(v -> this.showSessionInfo(session));
                    TextView del = this.text("\u5220\u9664", this.uiSize() - 1, this.muted());
                    del.setGravity(17);
                    line.addView((View)del, new LinearLayout.LayoutParams(this.dp(68.0f), this.dp(56.0f)));
                    del.setOnClickListener(v -> this.confirmDelete(() -> this.deleteSession(session.id)));
                    name.setOnClickListener(v -> this.loadSession(session.id));
                    items.addView(line);
                    View divider = new View(this);
                    divider.setBackgroundColor(AppearancePreferences.mutedAccent(this));
                    items.addView(divider, new LinearLayout.LayoutParams(-1, this.dp(1.0f)));
                }
                if (rows.isEmpty()) {
                    items.addView((View)this.text("\u8fd8\u6ca1\u6709\u5bf9\u8bdd\u8bb0\u5f55", this.uiSize(), this.muted()), new LinearLayout.LayoutParams(-1, this.dp(70.0f)));
                }
            });
        });
    }

    private List<ChatMessage> trimHistory(List<ChatMessage> full) {
        final String NL = String.valueOf((char) 10);
        // The user-authored system prompt leads every request. Inserted here rather than at
        // each call site so both the send path and any future caller get it for free.
        String sysPrompt = com.example.watchchat.data.PromptStore.systemPrompt(this);
        if (sysPrompt != null && !sysPrompt.isEmpty()) {
            java.util.List<ChatMessage> withSystem = new ArrayList<ChatMessage>(full.size() + 1);
            withSystem.add(new ChatMessage(java.util.UUID.randomUUID().toString(),
                    ChatMessage.Role.SYSTEM, sysPrompt, System.currentTimeMillis()));
            withSystem.addAll(full);
            full = withSystem;
        }
        int maxMessages = 20;
        int maxChars = 12000;
        if (full.size() <= 20) {
            int total = 0;
            for (ChatMessage msg : full) {
                total += msg.content.length();
            }
            if (total <= 12000) {
                return full;
            }
        }
        ArrayList<ChatMessage> out = new ArrayList<ChatMessage>();
        int total = 0;
        for (int start = full.size(); start > 0; --start) {
            ChatMessage msg = full.get(start - 1);
            int cost = msg.content.length();
            if (!out.isEmpty() && (out.size() >= 20 || total + cost > 12000)) break;
            out.add(0, msg);
            total += cost;
        }
        return out;
    }

    private void stop() {
        if (!this.generating) {
            return;
        }
        CancelHandle handle = this.active;
        this.finishRequest(this.requestSerial, this.requestTarget, this.answerRaw, null);
        ++this.requestSerial;
        this.generating = false;
        this.active = null;
        if (handle != null) {
            handle.cancel();
        }
    }

    private void updateStream(String value) {
        if (!(this.generating && this.chatVisible && this.sessionId.equals(this.requestTarget))) {
            return;
        }
        if (!this.inMessageColumn(this.streamBody)) {
            this.streamBody = new AssistantMessageView((Context)this, this.bodySize(), this.fg());
            LinearLayout wrap = this.col();
            wrap.addView(this.streamBody);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.rightMargin = this.dp(12.0f);
            p.bottomMargin = this.dp(12.0f);
            this.messageColumn.addView((View)wrap, p);
            VisualEffects.enter(wrap);
        }
        this.streamBody.streamRaw(value, this::followIfAllowed);
    }

    private void updateThinking(String value) {
        if (!(this.generating && this.chatVisible && this.sessionId.equals(this.requestTarget))) {
            return;
        }
        if (!this.inMessageColumn(this.thinkingWrap)) {
            this.thinkingWrap = this.col();
            this.thinkingWrap.setPadding(this.dp(10.0f), this.dp(8.0f), this.dp(10.0f), this.dp(8.0f));
            this.thinkingWrap.setBackground(this.shape(AppearancePreferences.panel(this), 10));
            TextView toggle = this.text("\u601d\u8003\u5185\u5bb9  \u25b8", this.uiSize() - 1, this.accent());
            this.thinkingWrap.addView(toggle);
            this.thinkingBody = this.text("", this.bodySize() - 1, this.muted());
            this.thinkingBody.setVisibility(this.reasoningExpanded ? 0 : 8);
            TextView currentThinkingBody = this.thinkingBody;
            toggle.setText(this.reasoningExpanded ? "\u601d\u8003\u5185\u5bb9  \u25be" : "\u601d\u8003\u5185\u5bb9  \u25b8");
            toggle.setOnClickListener(v -> {
                boolean expanded;
                this.reasoningExpanded = expanded = currentThinkingBody.getVisibility() != 0;
                currentThinkingBody.setVisibility(expanded ? 0 : 8);
                toggle.setText(expanded ? "\u601d\u8003\u5185\u5bb9  \u25be" : "\u601d\u8003\u5185\u5bb9  \u25b8");
                this.followIfAllowed();
            });
            this.thinkingWrap.addView(this.thinkingBody);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.bottomMargin = this.dp(6.0f);
            this.messageColumn.addView((View)this.thinkingWrap, p);
        }
        this.thinkingBody.setText(value);
        this.followIfAllowed();
    }

    private void addThinkingView(String value) {
        LinearLayout wrap = this.col();
        wrap.setPadding(this.dp(10.0f), this.dp(8.0f), this.dp(10.0f), this.dp(8.0f));
        wrap.setBackground(this.shape(AppearancePreferences.panel(this), 10));
        TextView lbl = this.text("\u601d\u8003\u5185\u5bb9  \u25b8", this.uiSize() - 1, this.accent());
        lbl.setPadding(this.dp(4.0f), this.dp(4.0f), this.dp(4.0f), this.dp(4.0f));
        wrap.addView(lbl);
        TextView body = this.text(value, this.bodySize() - 1, this.muted());
        body.setGravity(48);
        body.setTextIsSelectable(false);
        body.setVisibility(8);
        lbl.setOnClickListener(v -> {
            boolean show = body.getVisibility() != 0;
            body.setVisibility(show ? 0 : 8);
            lbl.setText(show ? "\u601d\u8003\u5185\u5bb9  \u25be" : "\u601d\u8003\u5185\u5bb9  \u25b8");
        });
        wrap.addView(body);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = this.dp(6.0f);
        this.messageColumn.addView((View)wrap, p);
    }

    private void finishRequest(long serial, String target, String answer, String error) {
        if (!this.ownsRequest(serial)) {
            return;
        }
        if (answer != null && !answer.isEmpty()) {
            this.updateStream(answer);
            if (this.inMessageColumn(this.streamBody)) {
                this.streamBody.setRaw(answer);
            }
        }
        this.generating = false;
        this.active = null;
        if (answer != null && !answer.isEmpty()) {
            ChatMessage assistant = ChatMessage.of(ChatMessage.Role.ASSISTANT, answer);
            if (!this.thoughtRaw.isEmpty()) {
                this.reasoningByMessage.put(assistant.id, this.thoughtRaw);
            }
            if (target.equals(this.sessionId)) {
                this.messages.add(assistant);
            }
            this.storage.execute(() -> this.repository.appendMessage(target, assistant));
        }
        this.streamBody = null;
        this.thinkingBody = null;
        this.thinkingWrap = null;
        this.updateRequestChrome();
        this.followIfAllowed();
        if (error != null && !this.isFinishing() && !this.isDestroyed()) {
            new AlertDialog.Builder(this).setMessage(error).setPositiveButton("\u786e\u5b9a", null).show();
        }
    }

    private void deleteSession(String id) {
        if (this.generating && id.equals(this.requestTarget)) {
            this.stop();
        }
        this.storage.execute(() -> {
            this.repository.deleteSession(id);
            List<SessionRepository.Session> remaining = this.repository.listSessions();
            String replacement = remaining.isEmpty() ? this.repository.createSession("\u65b0\u5bf9\u8bdd") : remaining.get((int)0).id;
            List<ChatMessage> loaded = this.repository.getMessages(replacement);
            this.runOnUiThread(() -> {
                this.sessionId = replacement;
                this.messages.clear();
                this.messages.addAll(loaded);
                if (this.historyVisible) {
                    this.showSessions();
                } else {
                    this.showChat();
                }
            });
        });
    }

    private void beginRequest(final long serial, final String target, final String requestKey, List<ChatMessage> history, ChatConfig requestConfig, RequestOptions requestOptions, List<Attachment> sendingImages) {
        try {
            // Captured once per request so onUsage can report the wall-clock duration.
            final long[] startedAt = { System.currentTimeMillis() };
            this.active = this.client.send(history, requestConfig, requestOptions, sendingImages, new ChatListener(){

                @Override
                public void onDelta(String delta) {
                    MainActivity.this.runOnUiThread(() -> {
                        if (MainActivity.this.ownsRequest(serial)) {
                            MainActivity.this.answerRaw = MainActivity.this.answerRaw + delta;
                            MainActivity.this.updateStream(MainActivity.this.answerRaw);
                        }
                    });
                }

                @Override
                public void onThinkingDelta(String delta) {
                    MainActivity.this.runOnUiThread(() -> {
                        if (MainActivity.this.ownsRequest(serial)) {
                            MainActivity.this.thoughtRaw = MainActivity.this.thoughtRaw + delta;
                            MainActivity.this.updateThinking(MainActivity.this.thoughtRaw);
                        }
                    });
                }

                @Override
                public void onUsage(ChatUsage usage) {
                    final long elapsed = System.currentTimeMillis() - startedAt[0];
                    MainActivity.this.runOnUiThread(() -> {
                        if (MainActivity.this.ownsRequest(serial)) {
                            MainActivity.this.storage.execute(() ->
                                    MainActivity.this.repository.recordUsage(target, requestKey, usage, elapsed));
                        }
                    });
                }

                @Override
                public void onStatus(String status) {
                    MainActivity.this.runOnUiThread(() -> {
                        if (MainActivity.this.ownsRequest(serial) && MainActivity.this.chatVisible && target.equals(MainActivity.this.sessionId)) {
                            MainActivity.this.activityStatus.setText(status);
                        }
                    });
                }

                @Override
                public void onComplete(String full) {
                    MainActivity.this.runOnUiThread(() -> MainActivity.this.finishRequest(serial, target, full, null));
                }

                @Override
                public void onError(ChatError error) {
                    MainActivity.this.runOnUiThread(() -> MainActivity.this.finishRequest(serial, target, MainActivity.this.answerRaw, "\u8bf7\u6c42\u5931\u8d25\uff1a" + error.getMessage()));
                }

                @Override
                public void onCancelled() {
                    MainActivity.this.runOnUiThread(() -> MainActivity.this.finishRequest(serial, target, MainActivity.this.answerRaw, null));
                }
            });
        }
        catch (Exception error) {
            this.finishRequest(serial, target, this.answerRaw, "\u8bf7\u6c42\u5931\u8d25\uff1a" + error.getMessage());
        }
    }

    private void save() {
    }


    /** Same press treatment as the settings page, so every tappable surface feels alike. */
    private void pressFeedback(android.view.View v) {
        if (v == null) return;
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().cancel();
                    view.animate().scaleX(0.97f).scaleY(0.97f)
                            .setDuration(100).setInterpolator(VisualEffects.EASE_OUT).start();
                    return false;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().cancel();
                    view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(140).setInterpolator(VisualEffects.EASE_OUT).start();
                    return false;
            }
            return false;
        });
    }

    /**
     * Opens the same report the history list's (i) button opens, but for the conversation the
     * user is currently reading. showSessionInfo guards on historyVisible because it was built
     * for that page; this path passes through it without that assumption.
     */
    private void openCurrentSessionInfo() {
        final String id = this.sessionId;
        if (id == null) {
            Hints.show(this, "当前还没有对话");
            return;
        }
        this.storage.execute(() -> {
            final SessionRepository.Session s = this.repository.getSession(id);
            this.runOnUiThread(() -> {
                if (this.isFinishing() || this.isDestroyed()) return;
                if (s == null) { Hints.show(this, "对话已删除"); return; }
                SessionInfoPanel panel = new SessionInfoPanel(this, s, this.uiSize(),
                        this.fg(), this.muted(), this.accent());
                panel.setOnRename(sel -> this.renameSession(sel, null));
                panel.show();
            });
        });
    }


    /**
     * Confirmation sheet, drawn in the app's own language.
     *
     * Why not AlertDialog: the platform dialog brings its own Material chrome, its own type
     * face and its own button metrics, so it reads as a different app and ignores the day/night
     * switch entirely. This is built from the same pieces as the model picker: a dimmed scrim,
     * a rounded card, and two buttons whose destructive one is the only coloured element.
     *
     * The destructive action is placed second and coloured; cancel is a quiet outline. That
     * ordering matters — a confirmation exists to stop a slip, so the safe choice must be the
     * easy one to hit.
     */
    private void confirmDelete(Runnable onConfirm) {
        if (this.root == null || this.isFinishing()) return;

        final android.app.Dialog dialog = new android.app.Dialog(this,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(AppearancePreferences.lightMode(this) ? 0x66FFFFFF : 0xAA000000);

        LinearLayout sheet = this.col();
        sheet.setPadding(this.dp(18.0f), this.dp(16.0f), this.dp(18.0f), this.dp(14.0f));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppearancePreferences.lightMode(this) ? 0xffffffff : 0xff141414);
        bg.setCornerRadius(this.dp(20.0f));
        bg.setStroke(Math.max(1, this.dp(1.0f)),
                AppearancePreferences.lightMode(this) ? 0xffdcdcd8 : 0x33FFFFFF);
        sheet.setBackground(bg);

        TextView title = this.text("删除对话", 15, this.fg());
        title.setTypeface(VisualEffects.type(this));
        sheet.addView((View)title, new LinearLayout.LayoutParams(-1, this.dp(26.0f)));

        TextView body = this.text("这个对话和它的全部消息将被永久删除，无法恢复。",
                12, this.muted());
        body.setPadding(0, 0, 0, this.dp(14.0f));
        sheet.addView((View)body, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout buttons = this.row();
        buttons.setGravity(gravityCenterForRow());

        TextView cancel = this.text("取消", 14, this.fg());
        cancel.setGravity(17);
        GradientDrawable cbg = new GradientDrawable();
        cbg.setColor(AppearancePreferences.lightMode(this) ? 0xffe9e9e5 : 0xff2A2A2C);
        cbg.setCornerRadius(this.dp(12.0f));
        cancel.setBackground(cbg);
        pressFeedback(cancel);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, this.dp(42.0f), 1.0f);
        clp.rightMargin = this.dp(6.0f);
        buttons.addView((View)cancel, clp);

        TextView confirm = this.text("删除", 14, 0xff1A0E0E);
        confirm.setGravity(17);
        GradientDrawable dbg = new GradientDrawable();
        dbg.setColor(0xffE5484D);
        dbg.setCornerRadius(this.dp(12.0f));
        confirm.setBackground(dbg);
        pressFeedback(confirm);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, this.dp(42.0f), 1.0f);
        dlp.leftMargin = this.dp(6.0f);
        buttons.addView((View)confirm, dlp);

        sheet.addView((View)buttons, new LinearLayout.LayoutParams(-1, this.dp(42.0f)));

        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(this.dp(240.0f), -2);
        flp.gravity = 17;
        overlay.addView((View)sheet, flp);
        dialog.setContentView(overlay);
        VisualEffects.applyFontToTree(overlay, this);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (screenW() * 0.92f), (int) (screenH() * 0.9f));
            w.setDimAmount(0f);
        }
        dialog.show();
        // Same entrance as the other sheets: grow from .94 and fade.
        if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
            sheet.setAlpha(0f);
            sheet.setScaleX(0.94f);
            sheet.setScaleY(0.94f);
            sheet.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(200L).setInterpolator(VisualEffects.EASE_OUT).start();
        }
    }

    private int gravityCenterForRow() {
        return android.view.Gravity.CENTER_VERTICAL;
    }

    private void showSessionInfo(SessionRepository.Session selected) {
        LinearLayout historyRoot = this.root;
        this.storage.execute(() -> {
            SessionRepository.Session fresh = this.repository.getSession(selected.id);
            this.runOnUiThread(() -> {
                if (this.isFinishing() || this.isDestroyed() || !this.historyVisible || this.root != historyRoot) {
                    return;
                }
                if (fresh == null) {
                    Hints.show(this, "\u5bf9\u8bdd\u5df2\u5220\u9664");
                    this.showSessions();
                    return;
                }
                SessionInfoPanel panel = new SessionInfoPanel(this, fresh, this.uiSize(), this.fg(), this.muted(), this.accent());
                panel.setOnRename(sel -> this.renameSession(sel, null));
                panel.show();
            });
        });
    }

    private void renameSession(SessionRepository.Session session, TextView name) {
        EditText input = new EditText(this);
        input.setTextSize(18.0f);
        input.setSingleLine(true);
        input.setText(name != null ? name.getText().toString() : session.title, TextView.BufferType.EDITABLE);
        input.setSelectAllOnFocus(true);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("\u4fee\u6539\u5bf9\u8bdd\u540d\u79f0").setView(input).setNegativeButton("\u53d6\u6d88", null).setPositiveButton("\u4fdd\u5b58", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                input.setError("\u8bf7\u8f93\u5165\u5bf9\u8bdd\u540d\u79f0");
                return;
            }
            this.storage.execute(() -> {
                try {
                    boolean saved = this.repository.renameSession(session.id, title);
                    this.runOnUiThread(() -> {
                        if (saved) {
                            if (name != null) {
                                name.setText(title);
                            }
                            dialog.dismiss();
                            if (this.historyVisible) {
                                this.showSessions();
                            } else {
                                this.storage.execute(() -> {
                                    List<SessionRepository.Session> refreshed = this.repository.listSessions();
                                    this.cachedSessions = refreshed;
                                });
                            }
                        } else {
                            input.setError("\u5bf9\u8bdd\u5df2\u88ab\u5220\u9664");
                        }
                    });
                }
                catch (Exception e) {
                    this.runOnUiThread(() -> input.setError("\u4fdd\u5b58\u5931\u8d25\uff1a" + e.getMessage()));
                }
            });
        }));
        dialog.show();
        input.requestFocus();
        input.post(() -> input.selectAll());
    }

    private void newSession() {
        this.stop();
        this.storage.execute(() -> {
            String id = this.repository.createSession("\u65b0\u5bf9\u8bdd");
            this.runOnUiThread(() -> {
                this.sessionId = id;
                this.messages.clear();
                this.showChat();
            });
        });
    }

    private void loadSession(String id) {
        this.stop();
        this.storage.execute(() -> {
            List<ChatMessage> loaded = this.repository.getMessages(id);
            this.runOnUiThread(() -> {
                this.sessionId = id;
                this.messages.clear();
                this.messages.addAll(loaded);
                this.showChat();
            });
        });
    }

    private void showImportChooser() {
        Dialog dialog = new Dialog(this, R.style.Theme_Black_NoTitleBar_Fullscreen);
        int dpv = this.dp(1.0f);
        LinearLayout panel = this.col();
        panel.setPadding(14 * dpv, 14 * dpv, 14 * dpv, 14 * dpv);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(-15724528);
        bg.setCornerRadius(this.dp(22.0f));
        bg.setStroke(Math.max(1, dpv), 0x33FFFFFF);
        panel.setBackground(bg);
        TextView title = this.text("\u5bfc\u5165", 18, WHITE);
        title.setGravity(17);
        title.setTypeface(VisualEffects.type(this));
        panel.addView((View)title, new LinearLayout.LayoutParams(-1, this.dp(38.0f)));
        TextView photo = this.text("\u76f8\u518c\u7167\u7247", 17, this.fg());
        photo.setGravity(17);
        VisualEffects.raisedSurface(photo, false);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(-1, this.dp(52.0f));
        p1.topMargin = this.dp(10.0f);
        panel.addView((View)photo, p1);
        TextView files = this.text("\u6587\u4ef6\u7ba1\u7406", 17, this.fg());
        files.setGravity(17);
        VisualEffects.raisedSurface(files, false);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(-1, this.dp(52.0f));
        p2.topMargin = this.dp(8.0f);
        panel.addView((View)files, p2);
        TextView cancel = this.text("\u53d6\u6d88", 15, this.muted());
        cancel.setGravity(17);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(-1, this.dp(40.0f));
        p3.topMargin = this.dp(6.0f);
        panel.addView((View)cancel, p3);
        VisualEffects.applyFontToTree(panel, this);
        dialog.setContentView(panel);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(this.dp(300.0f), this.dp(300.0f));
            w.setDimAmount(0.55f);
        }
        photo.setOnClickListener(v -> {
            dialog.dismiss();
            this.pickPhotoInApp();
        });
        files.setOnClickListener(v -> {
            dialog.dismiss();
            this.openFileBrowser();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == 9001 && this.openPicker != null) {
            this.openPicker.onPermissionResult();
        }
    }

    private Attachment loadImageAttachment(Uri uri, String name, String mime) {
        try {
            Bitmap source = ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), uri));
            if (source == null) {
                return null;
            }
            int max = 1024;
            int w = source.getWidth();
            int h = source.getHeight();
            float scale = Math.min(1.0f, (float)max / (float)Math.max(w, h));
            Bitmap scaled = w > max || h > max ? Bitmap.createScaledBitmap(source, (int)((float)w * scale), (int)((float)h * scale), true) : source;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int quality = 85;
            do {
                bytes.reset();
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, bytes);
            } while (bytes.size() > 524288 && (quality -= 15) > 30);
            String base64 = Base64.encodeToString(bytes.toByteArray(), 2);
            return Attachment.imageData(name, "image/jpeg", uri.toString(), base64);
        }
        catch (Exception e) {
            return null;
        }
    }

    private File safRootDir() {
        String saved;
        if (this.safRootDir == null && (saved = this.getSharedPreferences("file_features", 0).getString("saf_tree", null)) != null) {
            this.safTreeUri = Uri.parse(saved);
            try {
                String docId = DocumentsContract.getTreeDocumentId(this.safTreeUri);
                String rel = docId.contains(":") ? docId.split(":", 2)[1] : "";
                File base = new File("/storage/emulated/0");
                File dir = rel.isEmpty() ? base : new File(base, rel);
                this.safRootDir = dir.canRead() ? dir : null;
            }
            catch (Throwable t) {
                this.safRootDir = null;
            }
        }
        return this.safRootDir != null && this.safRootDir.canRead() ? this.safRootDir : null;
    }

    private void pickPhotoInApp() {
        ImagePickerDialog dialog;
        this.openPicker = dialog = new ImagePickerDialog(this, (uri, pickedName) -> {
            try {
                Attachment loaded = this.loadImageAttachment(uri, pickedName, this.getContentResolver().getType(uri));
                if (loaded != null) {
                    this.imageAttachments.put(uri.toString(), loaded);
                    this.refreshChatBody();
                    Hints.show(this, "\u5df2\u6dfb\u52a0\u56fe\u7247");
                } else {
                    Hints.show(this, "\u56fe\u7247\u8bfb\u53d6\u5931\u8d25");
                }
            }
            catch (Exception e) {
                Hints.show(this, "\u8bfb\u53d6\u5931\u8d25");
            }
        });
        dialog.setOnDismissListener(d -> {
            this.openPicker = null;
        });
        dialog.show();
    }

    private void openFileBrowser() {
        if (this.fileBrowser == null) {
            this.fileBrowser = new FileBrowser(this, new FileBrowser.Listener(){

                @Override
                public File treeRootDir() {
                    File saf = MainActivity.this.safRootDir();
                    if (saf != null) {
                        return saf;
                    }
                    File fb = MainActivity.this.getExternalFilesDir(null);
                    if (!fb.exists()) {
                        fb.mkdirs();
                    }
                    return fb;
                }

                @Override
                public void onPickTextFile(File f) {
                    try {
                        byte[] buf = new byte[(int)Math.min(f.length(), 262144L)];
                        FileInputStream in = new FileInputStream(f);
                        int n = in.read(buf);
                        in.close();
                        String content = new String(buf, 0, Math.max(0, n), StandardCharsets.UTF_8);
                        String block = "\n=== \u6587\u4ef6\uff1a" + f.getName() + " ===" + content + "\n=== \u7ed3\u675f ===";
                        MainActivity.this.composer.setText(MainActivity.this.composer.getText().toString() + block);
                        Hints.show(MainActivity.this, "\u5df2\u9644\u52a0\u6587\u4ef6\u5185\u5bb9\uff08\u524d256KB\uff09");
                    }
                    catch (Exception e) {
                        Hints.show(MainActivity.this, "\u8bfb\u53d6\u5931\u8d25\uff1a" + e.getMessage());
                    }
                }

                @Override
                public void onTreeGranted() {
                    MainActivity.this.safRootDir = null;
                    MainActivity.this.openFileBrowser();
                }

                @Override
                public void onUseAppDir() {
                    File fb = MainActivity.this.getExternalFilesDir(null);
                    if (!fb.exists()) {
                        fb.mkdirs();
                    }
                    MainActivity.this.safRootDir = fb;
                    Hints.show(MainActivity.this, "\u5df2\u5207\u6362\u5230\u5e94\u7528\u76ee\u5f55");
                }

                @Override
                public void requestTreeGrant() {
                    Intent i = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
                    MainActivity.this.startActivityForResult(i, 804);
                }
            }, this.uiSize(), this.fg(), this.muted(), this.accent());
        }
        this.fileBrowser.show();
    }

    private void showModelPicker() {
        ProviderStore.migrateLegacy(this);
        List<ProviderStore.Provider> providers = ProviderStore.list(this);
        String activeKey = ProviderStore.activeKey(this);
        Dialog dialog = new Dialog(this, R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout overlay = new FrameLayout(this);
        boolean lightPicker = AppearancePreferences.lightMode(this);
        overlay.setBackgroundColor(lightPicker ? 0x66FFFFFF : -1442840576);
        overlay.setOnClickListener(v -> dialog.dismiss());
        LinearLayout sheet = this.col();
        sheet.setPadding(this.dp(12.0f), this.dp(10.0f), this.dp(12.0f), this.dp(12.0f));
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(lightPicker ? 0xffffffff : 0xff141414);
        sheetBg.setCornerRadius(this.dp(22.0f));
        sheetBg.setStroke(Math.max(1, this.dp(1.0f)), 0x33FFFFFF);
        sheet.setBackground(sheetBg);
        sheet.setOnClickListener(v -> {});
        LinearLayout head = this.row();
        TextView title = this.text("\u9009\u62e9\u6a21\u578b", 14, this.fg());
        title.setTypeface(VisualEffects.type(this));
        head.addView((View)title, new LinearLayout.LayoutParams(0, this.dp(34.0f), 1.0f));
        TextView close = this.text("\u00d7", 18, this.muted());
        close.setGravity(17);
        close.setContentDescription("\u5173\u95ed");
        head.addView((View)close, new LinearLayout.LayoutParams(this.dp(34.0f), this.dp(34.0f)));
        sheet.addView((View)head, new LinearLayout.LayoutParams(-1, this.dp(34.0f)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = this.col();
        scroll.addView(list);
        sheet.addView((View)scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        int total = 0;
        for (ProviderStore.Provider p : providers) {
            if (p.models.isEmpty()) continue;
            TextView group = this.text(p.displayName, 11, this.muted());
            group.setPadding(this.dp(4.0f), this.dp(10.0f), 0, this.dp(4.0f));
            // WRAP_CONTENT, not a fixed 22dp: a CJK group name plus its own vertical padding
        // is taller than 22dp and was being clipped in half.
        list.addView((View)group, new LinearLayout.LayoutParams(-1, -2));
            for (String model : p.models) {
                ++total;
                boolean active = (p.id + "|" + model).equals(activeKey);
                LinearLayout card = this.row();
                card.setPadding(this.dp(12.0f), 0, this.dp(12.0f), 0);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(active ? (lightPicker ? 0xffebebe7 : 0xff2A2A2A) : 0);
                bg.setCornerRadius(this.dp(12.0f));
                card.setBackground(bg);
                TextView name = this.text(model, 13, active ? this.fg() : this.muted());
                name.setSingleLine(true);
                card.addView((View)name, new LinearLayout.LayoutParams(0, -2, 1.0f));
                // A glyph dot showed up as a stray white speck: the app face has no
                // U+25CF, so it rendered a missing-glyph box. Draw the marker instead.
                View dot = new View(this) {
                    @Override protected void onDraw(Canvas canvas) {
                        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
                        pt.setColor(AppearancePreferences.accent(getContext()));
                        if (getWidth() > 0) canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, dp(3f), pt);
                    }
                };
                card.addView((View)dot, new LinearLayout.LayoutParams(this.dp(20.0f), -2));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, this.dp(40.0f));
                lp.topMargin = this.dp(2.0f);
                list.addView((View)card, lp);
                card.setOnTouchListener((v, ev) -> {
                    if (ev.getAction() == 0) {
                        v.setAlpha(0.6f);
                    } else if (ev.getAction() == 1 || ev.getAction() == 3) {
                        v.setAlpha(1.0f);
                    }
                    return false;
                });
                card.setOnClickListener(v -> {
                    ProviderStore.setActive(this, p.id, model);
                    this.applyActiveModel();
                    v.performHapticFeedback(4);
                    sheet.animate().alpha(0.0f).scaleX(0.92f).scaleY(0.92f).setDuration(160L).setInterpolator(VisualEffects.EASE_OUT).withEndAction(() -> {
                        dialog.dismiss();
                        this.blurChat(false);
                        // Stay on the panel the user opened. Rebuilding the chat page here
                        // is what threw them back to the top of the conversation.
                        this.rebuildReasoningRow();
                    }).start();
                });
            }
        }
        if (total == 0) {
            TextView empty = this.text("\u8fd8\u6ca1\u6709\u53ef\u7528\u6a21\u578b\uff0c\u53bb\u8bbe\u7f6e\u91cc\u6dfb\u52a0", 12, this.muted());
            empty.setGravity(17);
            list.addView((View)empty, new LinearLayout.LayoutParams(-1, this.dp(60.0f)));
        }
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(-1, -1);
        slp.gravity = 17;
        slp.setMargins(this.dp(10.0f), this.dp(10.0f), this.dp(10.0f), this.dp(10.0f));
        overlay.addView((View)sheet, slp);
        dialog.setContentView(overlay);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            DisplayMetrics m = this.getResources().getDisplayMetrics();
            w.setLayout((int)((float)m.widthPixels * 0.94f), (int)((float)m.heightPixels * 0.88f));
            w.setDimAmount(0.0f);
        }
        VisualEffects.applyFontToTree(overlay, this);
        close.setOnClickListener(v -> dialog.dismiss());
        sheet.setPivotY(sheet.getHeight() > 0 ? (float)sheet.getHeight() : (float)this.dp(300.0f));
        sheet.setScaleX(0.9f);
        sheet.setScaleY(0.86f);
        sheet.setAlpha(0.0f);
        sheet.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(240L).setInterpolator(VisualEffects.EASE_OUT).start();
        dialog.setOnDismissListener(d -> this.blurChat(false));
        this.blurChat(true);
        dialog.show();
    }

    private void blurChat(boolean on) {
        if (this.root == null || Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            this.root.setRenderEffect(on ? RenderEffect.createBlurEffect(18.0f, 18.0f, Shader.TileMode.CLAMP) : null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void applyActiveModel() {
        ProviderStore.Provider p = ProviderStore.activeProvider(this);
        if (p != null) {
            this.config = new ChatConfig(ChatConfig.Provider.valueOf(ProviderStore.normalizeProtocol(p.protocol)), p.baseUrl, p.apiKey, ProviderStore.activeModel(this), 30000, 60000, 0);
        }
    }
}
