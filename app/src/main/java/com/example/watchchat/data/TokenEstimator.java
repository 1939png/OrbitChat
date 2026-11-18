package com.example.watchchat.data;

/**
 * Rough token count, used only for showing the composition of the context window.
 *
 * Why not a real tokenizer: shipping jtokkit or a BPE table costs ~2 MB of assets and a
 * second of cold-start time, to make a decorative bar 3% more accurate. Open-source chat
 * clients that show a context meter (Lobe Chat, Chatbox, LibreChat) all estimate locally
 * with a heuristic for exactly this reason, and reserve real tokenisation for the request
 * path where the number actually matters.
 *
 * The heuristic: CJK runs at ~1 token per character (each ideograph is typically one or two
 * BPE tokens; 1.0 is the middle of the measured range), Latin at ~4 characters per token
 * (OpenAI's own rule of thumb). Mixed strings are summed per class rather than averaged,
 * because a paragraph of Chinese with an English model name would otherwise be badly off.
 */
public final class TokenEstimator {

    private TokenEstimator() { }

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        int cjk = 0, other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) cjk++;
            else other++;
        }
        // ceil(other / 4) keeps short strings from rounding down to zero tokens.
        return cjk + (int) Math.ceil(other / 4.0);
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)     // unified ideographs
            || (c >= 0x3400 && c <= 0x4DBF)     // extension A
            || (c >= 0x3040 && c <= 0x30FF)     // kana
            || (c >= 0xAC00 && c <= 0xD7AF)     // hangul
            || (c >= 0x3000 && c <= 0x303F);    // CJK punctuation
    }
}
