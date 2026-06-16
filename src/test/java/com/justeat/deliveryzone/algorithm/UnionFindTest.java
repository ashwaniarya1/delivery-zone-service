package com.justeat.deliveryzone.algorithm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnionFindTest {

    @Test
    void singleton_finds_itself() {
        UnionFind uf = new UnionFind();
        uf.add("a");
        assertThat(uf.find("a")).isEqualTo("a");
    }

    @Test
    void simple_union_puts_two_ids_in_same_component() {
        UnionFind uf = new UnionFind();
        uf.add("a");
        uf.add("b");
        uf.union("a", "b");
        assertThat(uf.find("a")).isEqualTo(uf.find("b"));
    }

    @Test
    void transitive_chain_joins_all_three() {
        UnionFind uf = new UnionFind();
        uf.add("a");
        uf.add("b");
        uf.add("c");
        uf.union("a", "b");
        uf.union("b", "c");
        assertThat(uf.find("a")).isEqualTo(uf.find("c"));
    }

    @Test
    void union_is_idempotent() {
        UnionFind uf = new UnionFind();
        uf.add("a");
        uf.add("b");
        uf.union("a", "b");
        String rootAfterFirst = uf.find("a");
        uf.union("a", "b");
        assertThat(uf.find("a")).isEqualTo(rootAfterFirst);
    }

    @Test
    void isolated_nodes_remain_separate() {
        UnionFind uf = new UnionFind();
        uf.add("x");
        uf.add("y");
        uf.add("z");
        assertThat(uf.find("x")).isNotEqualTo(uf.find("y"));
        assertThat(uf.find("y")).isNotEqualTo(uf.find("z"));
    }

    @Test
    void partial_union_leaves_unrelated_node_isolated() {
        UnionFind uf = new UnionFind();
        uf.add("a");
        uf.add("b");
        uf.add("c");
        uf.union("a", "b");
        assertThat(uf.find("a")).isEqualTo(uf.find("b"));
        assertThat(uf.find("c")).isNotEqualTo(uf.find("a"));
    }
}
