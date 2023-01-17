package qp1.utilities;

import java.util.Arrays;

// Based on https://github.com/skittles1412/Battlecode2021/blob/master/src/utilities/IntHashMap.java

/**
 * An int -> int hashmap
 * 0 is the default value for nonexistent keys
 */
public class IntHashMap {
    private static final int INITIAL_BIN_CAPACITY = 4;
    private final int n;
//    private int size;
    private final int[] ind;
    private final int[][] keys, values;

    public IntHashMap(int bucketCount) {
        n = bucketCount;
        ind = new int[n];
        keys = new int[n][INITIAL_BIN_CAPACITY];
        values = new int[n][INITIAL_BIN_CAPACITY];
    }

    public void put(int key, int value) {
        int hash = key % n;
        int cind = ind[hash];
        int[] ckeys = keys[hash];
        int[] cvalues = values[hash];
        for (int i = cind; i --> 0;) {
            if (ckeys[i] == key) {
                cvalues[i] = value;
                return;
            }
        }
        if (cind == ckeys.length) {
            keys[hash] = Arrays.copyOf(keys[hash], ckeys.length * 2);
            values[hash] = Arrays.copyOf(values[hash], ckeys.length * 2);
            keys[hash][cind] = key;
            values[hash][cind] = value;
        } else {
            ckeys[cind] = key;
            cvalues[cind] = value;
        }
        ++ind[hash];
//        size++;
    }

    public int get(int key) {
        int hash = key % n;
        int[] ckeys = keys[hash];
        int[] cvalues = values[hash];
        for (int i = ind[hash]; i --> 0;) {
            if (ckeys[i] == key) {
                return cvalues[i];
            }
        }
        return 0;
    }

//    public void remove(int key) {
//        int hash = key % n;
//        int cind = ind[hash];
//        int[] ckeys = keys[hash];
//        int[] cvalues = values[hash];
//        for (int i = 0; i < cind; i++) {
//            if (ckeys[i] == key) {
//                System.arraycopy(ckeys, i + 1, ckeys, i, --ind[hash] - i);
//                System.arraycopy(cvalues, i + 1, cvalues, i, ind[hash] - i);
//                size--;
//                return;
//            }
//        }
//    }

    public static class Entry {
        public final int key, value;

        public Entry(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

//    public int size() {
//        return size;
//    }
//
//    public boolean isEmpty() {
//        return size == 0;
//    }
}
