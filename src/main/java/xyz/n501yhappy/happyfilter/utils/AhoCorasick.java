package xyz.n501yhappy.happyfilter.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class AhoCorasick {
    public void insert(String word) {
        Node cur = root;
        for (char c : word.toCharArray()) {
            cur = cur.next.computeIfAbsent(c, k -> new Node());
        }
        cur.word = word;
    }

    public void build() {
        Queue<Node> q = new ArrayDeque<>();
        root.fail = null;
        for (Node ch : root.next.values()) {
            ch.fail = root;
            q.add(ch);
        }
        while (!q.isEmpty()) {
            Node u = q.poll();
            for (Map.Entry<Character, Node> e : u.next.entrySet()) {
                char c = e.getKey();
                Node v = e.getValue();

                Node f = u.fail;
                while (f != null && !f.next.containsKey(c)) {
                    f = f.fail;
                }
                v.fail = (f == null) ? root : f.next.get(c);
                q.add(v);
            }
        }
    }
    public List<Hit> search(String text) {
        List<Hit> res = new ArrayList<>();
        Node cur = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (cur != root && !cur.next.containsKey(c)) {
                cur = cur.fail;
            }
            if (cur.next.containsKey(c)) {
                cur = cur.next.get(c);
            }
            for (Node p = cur; p != root && p.word != null; p = p.fail) {
                int len = p.word.length();
                res.add(new Hit(i - len + 1, i + 1));
            }
        }
        return res;
    }
    
    private static class Node {
        Map<Character, Node> next = new HashMap<>();
        Node fail;
        String word;
    }

    private final Node root = new Node();
    public static class Hit {//记住这是左闭右开喵！不要写错了喵！
        public final int start;
        public final int end;
        public Hit(int s, int e) { this.start = s; this.end = e; }
    }
}