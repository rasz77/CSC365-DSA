package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class BPlusTreeNode {
    boolean isLeaf;
    List<Integer> keys;
    List<BPlusTreeNode> children;
    List<String[]> values;
    BPlusTreeNode next;

    public BPlusTreeNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.values = new ArrayList<>();
        this.next = null;
    }
}

class BPlusTree {
    private BPlusTreeNode root;
    private final int order;

    public BPlusTree(int order) {
        if (order < 3) throw new IllegalArgumentException("Order must be at least 3");
        this.root = new BPlusTreeNode(true);
        this.order = order;
    }

    private BPlusTreeNode findLeaf(int key) {
        BPlusTreeNode node = root;
        while (!node.isLeaf) {
            int i = 0;
            while (i < node.keys.size() && key >= node.keys.get(i)) i++;
            node = node.children.get(i);
        }
        return node;
    }

    public void insert(int key, String[] data) {
        BPlusTreeNode leaf = findLeaf(key);
        int pos = Collections.binarySearch(leaf.keys, key);
        if (pos < 0) pos = -(pos + 1);
        leaf.keys.add(pos, key);
        leaf.values.add(pos, data);

        if (leaf.keys.size() > order - 1) {
            splitLeaf(leaf);
        }
    }

    private void splitLeaf(BPlusTreeNode leaf) {
        int mid = (order + 1) / 2;
        BPlusTreeNode newLeaf = new BPlusTreeNode(true);
        newLeaf.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        newLeaf.values.addAll(leaf.values.subList(mid, leaf.values.size()));
        leaf.keys.subList(mid, leaf.keys.size()).clear();
        leaf.values.subList(mid, leaf.values.size()).clear();

        newLeaf.next = leaf.next;
        leaf.next = newLeaf;

        if (leaf == root) {
            BPlusTreeNode newRoot = new BPlusTreeNode(false);
            newRoot.keys.add(newLeaf.keys.get(0));
            newRoot.children.add(leaf);
            newRoot.children.add(newLeaf);
            root = newRoot;
        } else {
            insertIntoParent(leaf, newLeaf, newLeaf.keys.get(0));
        }
    }

    private void insertIntoParent(BPlusTreeNode left, BPlusTreeNode right, int key) {
        BPlusTreeNode parent = findParent(root, left);
        if (parent == null) throw new RuntimeException("Parent not found");

        int pos = Collections.binarySearch(parent.keys, key);
        if (pos < 0) pos = -(pos + 1);
        parent.keys.add(pos, key);
        parent.children.add(pos + 1, right);

        if (parent.keys.size() > order - 1) {
            splitInternal(parent);
        }
    }

    private void splitInternal(BPlusTreeNode node) {
        int mid = (order + 1) / 2;
        BPlusTreeNode newInternal = new BPlusTreeNode(false);

        newInternal.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        newInternal.children.addAll(node.children.subList(mid + 1, node.children.size()));

        int midKey = node.keys.get(mid);

        node.keys.subList(mid, node.keys.size()).clear();
        node.children.subList(mid + 1, node.children.size()).clear();

        if (node == root) {
            BPlusTreeNode newRoot = new BPlusTreeNode(false);
            newRoot.keys.add(midKey);
            newRoot.children.add(node);
            newRoot.children.add(newInternal);
            root = newRoot;
        } else {
            insertIntoParent(node, newInternal, midKey);
        }
    }

    private BPlusTreeNode findParent(BPlusTreeNode current, BPlusTreeNode target) {
        if (current.isLeaf || current.children.isEmpty()) return null;
        for (int i = 0; i < current.children.size(); i++) {
            BPlusTreeNode child = current.children.get(i);
            if (child == target) return current;
            BPlusTreeNode result = findParent(child, target);
            if (result != null) return result;
        }
        return null;
    }

    public String[] search(String keyStr) {
            int key = Integer.parseInt(keyStr);
            BPlusTreeNode node = findLeaf(key);
            int pos = Collections.binarySearch(node.keys, key);
            return pos >= 0 ? node.values.get(pos) : null;
    }

    public boolean search(int key) {
        BPlusTreeNode node = findLeaf(key);
        int pos = Collections.binarySearch(node.keys, key);
        return pos >= 0;
    }

    public void writeTreeStructureToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            Queue<BPlusTreeNode> queue = new LinkedList<>();
            queue.add(root);
            int level = 0;

            writer.write("--- B+ Tree Structure ---");
            while (!queue.isEmpty()) {
                int size = queue.size();
                writer.write("\nLevel " + level + ": ");
                for (int i = 0; i < size; i++) {
                    BPlusTreeNode node = queue.poll();
                    writer.write("[");
                    for (int j = 0; j < node.keys.size(); j++) {
                        writer.write(String.valueOf(node.keys.get(j)));
                        if (j < node.keys.size() - 1) writer.write(", ");
                    }
                    writer.write("] ");
                    if (!node.isLeaf) {
                        queue.addAll(node.children);
                    }
                }
                writer.write("");
                        level++;
            }

                    writer.write("-------------------------");
        }
    }
}
