public class Main {
    public static void main(String[] args) {
        var hashTable = new HashTable<String,String>();

        hashTable.set("hello","world");
        hashTable.set("Marco","Polo");
        hashTable.set("My","loving");
        hashTable.set("hash","table");
        hashTable.set("is","awesome");

        hashTable.set("Truffle","Ruby");
        hashTable.set(" ","?");
        hashTable.set("Great","Language");

        hashTable.set("final key","final value");

        System.out.println("Values inserted are :");
        System.out.println(hashTable.get("hello"));
        System.out.println(hashTable.get("Marco"));
        System.out.println(hashTable.get("My"));
        System.out.println(hashTable.get("hash"));
        System.out.println(hashTable.get("is"));
        System.out.println(hashTable.get("Truffle"));
        System.out.println(hashTable.get(" "));
        System.out.println(hashTable.get("Great"));
        System.out.println(hashTable.get("final key"));
        System.out.println(hashTable.get("No key"));

        hashTable.rm("Marco");
        hashTable.rm("is");
        hashTable.rm("final key");

        System.out.println("Now keys are :");
        System.out.println(hashTable.get("hello"));
        System.out.println(hashTable.get("Marco"));
        System.out.println(hashTable.get("My"));
        System.out.println(hashTable.get("hash"));
        System.out.println(hashTable.get("is"));
        System.out.println(hashTable.get("Truffle"));
        System.out.println(hashTable.get(" "));
        System.out.println(hashTable.get("Great"));
        System.out.println(hashTable.get("final key"));
        System.out.println(hashTable.get("No key"));
    }
}