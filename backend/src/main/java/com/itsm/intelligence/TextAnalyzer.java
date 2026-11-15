package com.itsm.intelligence;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight natural-language processing: tokenising, stop-word removal, crude stemming,
 * synonym folding and cosine similarity over term-frequency vectors.
 */
public final class TextAnalyzer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "being", "to", "of",
            "in", "on", "at", "for", "with", "from", "by", "as", "it", "its", "this", "that", "these", "those",
            "i", "me", "my", "we", "our", "you", "your", "he", "she", "they", "them", "his", "her", "their",
            "am", "do", "does", "did", "has", "have", "had", "will", "would", "can", "could", "should", "may",
            "not", "no", "so", "if", "then", "than", "there", "here", "when", "while", "since", "also", "very",
            "just", "any", "some", "all", "please", "help", "hi", "hello", "team", "issue", "problem", "facing",
            "get", "getting", "got", "make", "making", "today", "now", "again", "still", "into", "about", "after");

    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        synonyms("unavailable", "down", "outage", "unreachable", "offline");
        synonyms("fail", "failure", "error", "broken", "fault", "crash");
        synonyms("unable", "cannot", "cant", "can't");
        synonyms("login", "logon", "signin", "log-in", "sign-in");
        synonyms("payment", "pay", "paying");
        synonyms("slow", "latency", "lag", "sluggish");
        synonyms("password", "passwd", "pwd");
        synonyms("email", "mail", "outlook");
        synonyms("wifi", "wi-fi", "wireless");
    }

    private TextAnalyzer() {
    }

    private static void synonyms(String canonical, String... variants) {
        for (String v : variants) {
            SYNONYMS.put(v, canonical);
        }
    }

    public static String normalise(String token) {
        String t = token.toLowerCase(Locale.ROOT);
        t = SYNONYMS.getOrDefault(t, t);
        t = stem(t);
        return SYNONYMS.getOrDefault(t, t);
    }

    static String stem(String word) {
        if (word.length() > 5 && word.endsWith("ing")) {
            return undouble(word.substring(0, word.length() - 3));
        }
        if (word.length() > 4 && word.endsWith("ed")) {
            return undouble(word.substring(0, word.length() - 2));
        }
        if (word.length() > 4 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.length() > 4 && (word.endsWith("shes") || word.endsWith("ches") || word.endsWith("xes")
                || word.endsWith("sses"))) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    /** "dropp" -> "drop", "logg" -> "log"; keeps "ll" and "ss" as in "install", "access". */
    private static String undouble(String stem) {
        int n = stem.length();
        if (n > 2 && stem.charAt(n - 1) == stem.charAt(n - 2) && "bgmnprt".indexOf(stem.charAt(n - 1)) >= 0) {
            return stem.substring(0, n - 1);
        }
        return stem;
    }

    /** Term-frequency map of meaningful, normalised tokens. */
    public static Map<String, Integer> termFrequencies(String text) {
        Map<String, Integer> tf = new HashMap<>();
        if (text == null) {
            return tf;
        }
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9']+")) {
            String token = raw.replace("'", "");
            if (token.length() < 2 || STOP_WORDS.contains(token)) {
                continue;
            }
            tf.merge(normalise(token), 1, Integer::sum);
        }
        return tf;
    }

    public static Set<String> tokens(String text) {
        return termFrequencies(text).keySet();
    }

    public static double cosineSimilarity(Map<String, Integer> a, Map<String, Integer> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        double dot = 0;
        for (var entry : a.entrySet()) {
            Integer other = b.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        return dot / (norm(a) * norm(b));
    }

    public static double similarity(String a, String b) {
        return cosineSimilarity(termFrequencies(a), termFrequencies(b));
    }

    private static double norm(Map<String, Integer> v) {
        double sum = 0;
        for (int x : v.values()) {
            sum += (double) x * x;
        }
        return Math.sqrt(sum);
    }
}
