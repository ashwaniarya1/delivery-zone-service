package com.justeat.deliveryzone.algorithm;

import java.util.HashMap;
import java.util.Map;

public class UnionFind {

    private final Map<String, String> parent = new HashMap<>();
    private final Map<String, Integer> rank = new HashMap<>();

    public void add(String id) {
        parent.putIfAbsent(id, id);
        rank.putIfAbsent(id, 0);
    }

    public String find(String id) {
        if (!parent.containsKey(id)) {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        if (!parent.get(id).equals(id)) {
            parent.put(id, find(parent.get(id)));
        }
        return parent.get(id);
    }

    public void union(String a, String b) {
        String rootA = find(a);
        String rootB = find(b);
        if (rootA.equals(rootB)) return;
        int rankA = rank.get(rootA);
        int rankB = rank.get(rootB);
        if (rankA < rankB) {
            parent.put(rootA, rootB);
        } else if (rankA > rankB) {
            parent.put(rootB, rootA);
        } else {
            parent.put(rootB, rootA);
            rank.put(rootA, rankA + 1);
        }
    }
}
