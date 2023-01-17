package qp1_5_carrierlauncherspam.utilities;

import java.util.Arrays;
import java.util.Iterator;

// Based on https://github.com/skittles1412/Battlecode2021/blob/master/src/utilities/IntHashMap.java

/**
 * An int -> int hashmap
 * 0 is the default value for nonexistent keys
 */
public class IntHashMap implements Iterable<IntHashMap.Entry> {
    private static final int INITIAL_BIN_CAPACITY = 4;
    private final int n;
    private int size;
    private final int[] ind;
    private final int[][] keys, values;

    public IntHashMap(int initialCapacity) {
        n = initialCapacity;
        ind = new int[n];
        keys = new int[n][INITIAL_BIN_CAPACITY];
        values = new int[n][INITIAL_BIN_CAPACITY];
    }

    public void put(int key, int value) {
        int hash = key % n;
        int cind = ind[hash];
        int[] ckeys = keys[hash];
        int[] cvalues = values[hash];
        for (int i = 0; i < cind; i++) {
            if (ckeys[i] == key) {
                cvalues[i] = value;
                return;
            }
        }
        size++;
        int len;
        if (cind == (len = ckeys.length)) {
            keys[hash] = Arrays.copyOf(keys[hash], len *= 2);
            values[hash] = Arrays.copyOf(values[hash], len);
            keys[hash][cind] = key;
            values[hash][ind[hash]++] = value;
        } else {
            ckeys[cind] = key;
            cvalues[ind[hash]++] = value;
        }
    }

    public int get(int key) {
        int hash = key % n;
        int cind = ind[hash];
        int[] ckeys = keys[hash];
        int[] cvalues = values[hash];
        for (int i = 0; i < cind; i++) {
            if (ckeys[i] == key) {
                return cvalues[i];
            }
        }
        return 0;
    }

    public void remove(int key) {
        int hash = key % n;
        int cind = ind[hash];
        int[] ckeys = keys[hash];
        int[] cvalues = values[hash];
        for (int i = 0; i < cind; i++) {
            if (ckeys[i] == key) {
                System.arraycopy(ckeys, i + 1, ckeys, i, --ind[hash] - i);
                System.arraycopy(cvalues, i + 1, cvalues, i, ind[hash] - i);
                size--;
                return;
            }
        }
    }

    public static class Entry {
        public final int key, value;

        public Entry(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    private class Itr implements Iterator<Entry> {
        int binInd = 0;//index of last bin that was accessed
        int nodeInd = -1;//index of the last node in that bin that was accessed

        @Override
        public boolean hasNext() {
            if (binInd < n) {
                if (nodeInd + 1 >= ind[binInd]) {
                    while (++binInd < n && ind[binInd] == 0) ;
                    nodeInd = -1;
                }
            }
            return binInd < n;
        }

        @Override
        public Entry next() {
            if (binInd < n) {
                if (++nodeInd >= ind[binInd]) {
                    while (++binInd < n && ind[binInd] == 0) ;
                    nodeInd = 0;
                }
            }
            return new Entry(keys[binInd][nodeInd], values[binInd][nodeInd]);
        }
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Itr();
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
