package com.example.watchchat;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

/** Display-only formatter. Call only for assistant TextViews; persist the original raw text. */
public final class MessageFormatter {
    private MessageFormatter() {}

    /** Returns native spans, with no network, WebView, HTML interpretation, or storage access. */
    public static CharSequence format(String raw) {
        MessageFormatCore.Result result = MessageFormatCore.parse(raw);
        SpannableString text = new SpannableString(result.text);
        for (MessageFormatCore.Mark mark : result.marks) {
            Object span = mark.kind == MessageFormatCore.CODE ? new TypefaceSpan("monospace")
                    : mark.kind == MessageFormatCore.HEADING ? new RelativeSizeSpan(1.15f)
                    : new StyleSpan(Typeface.BOLD);
            text.setSpan(span, mark.start, mark.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return text;
    }
}
