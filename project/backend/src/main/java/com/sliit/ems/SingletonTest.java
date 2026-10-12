
package com.sliit.ems;

public class SingletonTest {
    public static void main(String[] args) {
        DatabaseConnectionManager db1 = DatabaseConnectionManager.getInstance();

        DatabaseConnectionManager db2 = DatabaseConnectionManager.getInstance();

        System.out.println("First instance: " + db1);
        System.out.println("Second instance: " + db2);
        System.out.println("Same instance? " + (db1 == db2));
    }
}
