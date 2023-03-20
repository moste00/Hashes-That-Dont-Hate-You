import java.util.Arrays;
import java.util.BitSet;

public class HashTable<K,V> {

    //The index array consists of "Slots", each slot is 2 array positions, the first holds a hash and the second holds an offseted index into the KV store (index + 1)
    //Slots always begin at even positions, so index[0] and index[1] is one slot, index[2] and index[3] is another, etc...
    //A Slot can be "marked" a certain state by setting the second position to an invalid index value (0, -1, -2, etc...)
    private int[] index ;
    //A slot in the index can be in one of 3 states, depending on the value of its second field
    //State 1 : Filled, the data in the hash field and the index field is valid, in this case the index field lies in 1..MAX_INT
    //State 2 : Empty, the data in the hash field and the index field is NOT valid, and the slot was never filled with valid data
    private static final int INDEX_SLOT_EMPTY = 0;
    //State 3 : Deleted, the data in the hash field and the index field is NOT valid, but the slot used to be occupied with valid data before
    private static final int INDEX_SLOT_DELETED = -1;

    //A plain old growable array with both a capacity and a size, capacity is the .length field of the array
    private Object[] kvStore;
    private int kvSize;


    public static final int DEFAULT_INITIAL_CAPACITY = 8 ;
    public static final float THRESHOLD_LOAD_FACTOR_FOR_INDEX_REBUILD = 0.65f;
    public HashTable(int initialCapacity) {
        if (initialCapacity < DEFAULT_INITIAL_CAPACITY) initialCapacity = DEFAULT_INITIAL_CAPACITY ;

        //All 0s be default, all slots marked empty for free
        this.index = new int[2*initialCapacity];

        this.kvStore = new Object[2*initialCapacity];
        //The actually occupied space (i.e. all data at indices from 0 to kvSize excluding kvSize itself are valid data)
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
        int i = getKvPosForHash(key.hashCode());
        //hash doesn't exist, so key definitely doesn't exist
        if (i == -1) return null;

        //otherwise, there is **something** that hashes to key.hashcode() that exist in our KV store, but it's not necessarily the key since hashes can collide
        //noinspection unchecked
        K k = (K) kvStore[i];   //no worries about the cast
        //noinspection unchecked
        V v = (V) kvStore[i+1]; //it's okay, we know for a fact that it's of type V, the set method is type-safe and nobody else can put in the KvStore

        //fast identity check, implies equality but avoids a potentially slow .equal() call
        if (k == key) return v;
        //identity is not equal, but it's still possible for equality to hold
        if (key.equals(k)) return v;

        //The key is not actually in the KV store and the hash we found above was indeed a collision
        return null;
    }

    private int getKvPosForHash(int hash) {
        int hashPosInIndex = lookupIndex(index,hash);
        if (hashPosInIndex == -1) return -1;

        return index[hashPosInIndex+1] - 1;
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

        insertIndex(index,hash,i);
    }
    private void rebuildIndex() {
        int[] newIndex = new int[2*index.length];

        for (int i = 0; i<index.length; i += 2) {
            boolean slotEmptyOrDeleted = index[i+1] <= INDEX_SLOT_EMPTY;

            if (!slotEmptyOrDeleted) { //i.e. it's filled with valid data
                insertIndex(newIndex,index[i],index[i+1]-1);
            }
        }
        index = newIndex;
    }

    private static void insertIndex(int[] index, int hash, int i) {
        int insertionPos = Math.abs(hash % index.length);
        //make it even, because slots are always aligned on even indices
        insertionPos = insertionPos & 0xFF_FF_FF_FE; //ensure the last bit is 0, meaning an even number, and leave the rest unaffected

        //linear probing on finding out that the chosen slot is already taken
        boolean slotEmptyOrDeleted = index[insertionPos+1] <= INDEX_SLOT_EMPTY;
        while (!slotEmptyOrDeleted) {
            insertionPos = (insertionPos+2) % index.length;
            slotEmptyOrDeleted = index[insertionPos+1] <= INDEX_SLOT_EMPTY;
        }
        index[insertionPos] = hash;
        index[insertionPos+1] = i+1;
    }

    private static int lookupIndex(int[] index,int hashCode) {
        int pos = Math.abs(hashCode % index.length);
        pos = pos & 0xFF_FF_FF_FE;

        boolean slotEmpty = index[pos+1] == INDEX_SLOT_EMPTY;
        boolean slotDeleted = index[pos+1] == INDEX_SLOT_DELETED;
        int firstDeletedSlot = -1;

        while (!slotEmpty) {
            if (slotDeleted) {
                if (firstDeletedSlot == -1) firstDeletedSlot = pos;
            }
            //slot is neither empty nor deleted, i.e. it's filled, i.e. let's see the hash there
            else {
                //found our guy
                if (index[pos] == hashCode) break;
            }

            //If the slot was deleted or it has a different hash from the one we're looking for, keep going
            pos = (pos+2) % index.length;
            slotEmpty = index[pos+1] == INDEX_SLOT_EMPTY;
            slotDeleted = index[pos+1] == INDEX_SLOT_DELETED;
        }
        if (slotEmpty) return -1;

        //relocate the found element to the first deleted slot so that we find it faster the next time
        if (firstDeletedSlot != -1) {
            index[firstDeletedSlot] = hashCode;
            index[firstDeletedSlot+1] = index[pos+1];
            index[pos+1] = INDEX_SLOT_DELETED;
        }
        return pos;
    }
}