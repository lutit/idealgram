package org.telegram.messenger;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EpsteinMode {

    private static final char BLOCK = '\u2588';

    private static final String[] NEVER_OBFUSCATE_WORDS = new String[] {
            "epstein",
            "uzbek",
            "uzbekgram",
            "узбекграм"
    };

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{Mn}\\p{Nd}_']+");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|tg://|ftp://|www\\.)\\S+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b\\S+@\\S+\\b");
    private static final Pattern MENTION_OR_HASHTAG_PATTERN = Pattern.compile("(?<!\\S)[@#][\\p{L}\\p{Nd}_]{2,}");

    private EpsteinMode() {
    }

    public static String obfuscateText(String text) {
        return obfuscateText(text, System.currentTimeMillis() ^ Utilities.fastRandom.nextLong());
    }

    public static String obfuscateText(String text, long seed) {
        if (TextUtils.isEmpty(text)) {
            return text;
        }

        final ArrayList<IntRange> protectedRanges = new ArrayList<>(4);
        collectRanges(protectedRanges, URL_PATTERN, text);
        collectRanges(protectedRanges, EMAIL_PATTERN, text);
        collectRanges(protectedRanges, MENTION_OR_HASHTAG_PATTERN, text);

        final ArrayList<WordSpan> words = new ArrayList<>();
        final Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();
            if (intersectsAny(protectedRanges, start, end)) {
                continue;
            }
            final String word = text.substring(start, end);
            if (!isEligibleWord(word)) {
                continue;
            }
            words.add(new WordSpan(start, end, word));
        }

        if (words.isEmpty()) {
            return text;
        }

        final Random random = new Random(seed);

        final boolean[] masked = new boolean[words.size()];
        final int eligibleCount = words.size();

        int target = Math.round(eligibleCount * 0.45f);
        target = Math.max(1, target);

        final int minVisible = eligibleCount >= 4 ? 2 : 1;
        target = Math.min(target, eligibleCount - minVisible);
        if (target <= 0) {
            return text;
        }

        final int firstProtected = eligibleCount >= 3 ? 0 : -1;
        final int lastProtected = eligibleCount >= 3 ? eligibleCount - 1 : -1;

        final ArrayList<Integer> candidates = new ArrayList<>(eligibleCount);
        for (int i = 0; i < eligibleCount; i++) {
            if (i == firstProtected || i == lastProtected) {
                continue;
            }
            candidates.add(i);
        }

        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Integer tmp = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, tmp);
        }

        int chosen = 0;
        for (int index : candidates) {
            if (chosen >= target) {
                break;
            }
            if (wouldCreateLongMaskedRun(masked, index)) {
                continue;
            }
            masked[index] = true;
            chosen++;
        }

        if (chosen == 0) {
            return text;
        }

        final StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        for (int i = 0; i < words.size(); i++) {
            WordSpan span = words.get(i);
            if (cursor < span.start) {
                out.append(text, cursor, span.start);
            }
            if (masked[i]) {
                out.append(blocksFor(span.word.length()));
            } else {
                out.append(text, span.start, span.end);
            }
            cursor = span.end;
        }
        if (cursor < text.length()) {
            out.append(text, cursor, text.length());
        }

        return out.toString();
    }

    private static boolean isEligibleWord(String word) {
        if (TextUtils.isEmpty(word)) {
            return false;
        }

        final String lower = word.toLowerCase(Locale.ROOT);
        for (String never : NEVER_OBFUSCATE_WORDS) {
            if (lower.equals(never)) {
                return false;
            }
        }

        boolean hasLetter = false;
        boolean allDigits = true;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (!Character.isDigit(c)) {
                allDigits = false;
            }
        }
        if (!hasLetter || allDigits) {
            return false;
        }

        return word.length() >= 3;
    }

    private static String blocksFor(int wordLength) {
        int count = wordLength;
        if (count < 3) {
            count = 3;
        } else if (count > 12) {
            count = 12;
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(BLOCK);
        }
        return sb.toString();
    }

    private static boolean wouldCreateLongMaskedRun(boolean[] masked, int index) {
        int run = 1;
        for (int i = index - 1; i >= 0 && masked[i]; i--) {
            run++;
        }
        for (int i = index + 1; i < masked.length && masked[i]; i++) {
            run++;
        }
        return run >= 3;
    }

    private static void collectRanges(ArrayList<IntRange> ranges, Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            ranges.add(new IntRange(m.start(), m.end()));
        }
    }

    private static boolean intersectsAny(ArrayList<IntRange> ranges, int start, int end) {
        for (int i = 0, size = ranges.size(); i < size; i++) {
            IntRange r = ranges.get(i);
            if (start < r.end && end > r.start) {
                return true;
            }
        }
        return false;
    }

    private static final class IntRange {
        final int start;
        final int end;

        IntRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class WordSpan {
        final int start;
        final int end;
        final String word;

        WordSpan(int start, int end, String word) {
            this.start = start;
            this.end = end;
            this.word = word;
        }
    }
}

