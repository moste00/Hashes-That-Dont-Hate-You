import java.util.BitSet;

public class HashTable<K,V> {

    private int[] index ;
    private BitSet indexSlotFilled;
    private Object[] kvStore;
    int kvSize;

    public static final int DEFAULT_INITIAL_CAPACITY = 8 ;
    public static final float THRESHOLD_LOAD_FACTOR_FOR_INDEX_REBUILD = 0.65f;
    public HashTable(int initialCapacity) {
        if (initialCapacity < DEFAULT_INITIAL_CAPACITY) initialCapacity = DEFAULT_INITIAL_CAPACITY ;

        this.index = new int[2*initialCapacity];
        //The ith bit is a flag for whether the slot at {H: index[2*i],I: index[2*i+1]} is filled or not
        this.indexSlotFilled = new BitSet(initialCapacity);

        this.kvStore = new Object[2*initialCapacity];
        //number of entries in the kvStore, i.e. the length of the actually occupied space (i.e. all indices from 0 to kvSize excluding kvSize are valid data)
        //always increases by 2
        this.kvSize = 0;
    }

    public HashTable() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public void set(K key,V value) {
        int i = kvSize;
        insertKvStore(key,value);
        insertIndex(key.hashCode(),i);
    }

    public V get(K key) {
        int hashPosInIndex = lookupIndex(key.hashCode());
        if (hashPosInIndex == -1) return null;

        int i = index[hashPosInIndex+1];
        //noinspection unchecked
        K k = (K) kvStore[i];   //no worries
        //noinspection unchecked
        V v = (V) kvStore[i+1]; //it's okay, we know for a fact that it's of type V, the set method is type-safe
        
        //fast identity check, implies equality but avoids a potentially slow .equal() call
        if (k == key) return v;
        //identity not equal, still possible for equality to hold
        if (key.equals(k)) return v;

        //The key is not actually in the KV store and the hash we found above was a collision
        return null;
    }

    private void insertKvStore(Object k, Object v) {
        if (kvSize >= kvStore.length-1) resizeKvStore();

        kvStore[kvSize] = k;
        kvStore[kvSize+1] = v;
        kvSize += 2;
    }
    private void resizeKvStore() {
        Object[] newKvStore = new Object[2*kvStore.length];
        System.arraycopy(kvStore, 0, newKvStore, 0, kvSize);
        kvStore = newKvStore;
    }

    private void insertIndex(int hash, int i) {
        float currentLoadFactor = ((float)kvSize)/index.length;
        if (currentLoadFactor > THRESHOLD_LOAD_FACTOR_FOR_INDEX_REBUILD) rebuildIndex();

        insertIndex(index,indexSlotFilled,hash,i);
    }
    private void rebuildIndex() {
        int[] newIndex = new int[2*index.length];
        BitSet newIndexSlotFilled = new BitSet(2*indexSlotFilled.size());

        for (int i = 0; i<index.length; i += 2) {
            if (indexSlotFilled.get(i/2)) {
                insertIndex(newIndex,newIndexSlotFilled,index[i],index[i+1]);
            }
        }

        index = newIndex;
        indexSlotFilled = newIndexSlotFilled;
    }

    private static void insertIndex(int[] index, BitSet indexSlotFilled, int hash, int i) {
        int insertionPos = Math.abs(hash % index.length);
        //make it even, because slots are always aligned on even indices
        insertionPos = insertionPos & 0xFF_FF_FF_FE; //ensure the last bit is 0, meaning an even number, and leave the rest unaffected

        //linear probing on finding out that the chosen slot is already taken
        while (indexSlotFilled.get(insertionPos/2)) {
            insertionPos = (insertionPos+2) % index.length;
        }
        indexSlotFilled.set(insertionPos/2);
        index[insertionPos] = hash;
        index[insertionPos+1] = i;
    }

    private int lookupIndex(int hashCode) {
        int pos = Math.abs(hashCode % index.length);
        pos = pos & 0xFF_FF_FF_FE;

        while (indexSlotFilled.get(pos/2) && index[pos] != hashCode) {
            pos = (pos+2) % index.length;
        }
        if (indexSlotFilled.get(pos/2)) {
            return pos;
        }
        else {
            return -1;
        }
    }

}
