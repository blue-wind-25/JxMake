package com.example.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.example.Base;

import com.example.util.Helper;

// A basic Java class exercising core formatting rules (Java 8-compatible constructs)

public class CoreExample extends Base implements Runnable, Cloneable {

    private static final int     MAX_COUNT = 100;
    private static final String  PREFIX    = "item_";
    private              int     count;
    private              String  name;
    private              boolean active;
    private              long    timestamp;

    // Constructor with closing comment test (short -- no comment expected)
    public CoreExample(int count, String name)
    {
        this.count     = count;
        this.name      = name;
        this.active    = true;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor with closing comment test (long -- comment expected)
    public CoreExample(int count, String name, boolean active, long timestamp, String extra)
    {
        this.count     = count;
        this.name      = name;
        this.active    = active;
        this.timestamp = timestamp;
    }

    // Simple getters and setters (should be grouped as one-liners if short)
    public int     getCount    (              ) { return count;         }
    public void    setCount    (int     count ) { this.count = count;   }
    public String  getName     (              ) { return name;          }
    public void    setName     (String  name  ) { this.name = name;     }
    public boolean isActive    (              ) { return active;        }
    public void    setActive   (boolean active) { this.active = active; }
    public long    getTimestamp(              ) { return timestamp;     }

    // Method with K&R brace style for control flow, Allman for the method itself
    public void process(List<String> items) throws IOException
    {
        if(items == null) throw new IllegalArgumentException("items must not be null");
        for( int i = 0; i < items.size(); ++i ) {
            String item = items.get(i);
            if( item.startsWith(PREFIX) ) {
                System.out.println(item);
            }
            else {
                System.err.println("skipping: " + item);
            }
        } // for
        // While loop
        int idx = 0;
        while(idx < MAX_COUNT) idx++;
        // Do-while
        do {
            --idx;
        } while(idx > 0);
    }

    // Switch statement
    public String describe(int code)
    {
        switch(code) {
            case 1  : return "one"          ;
            case 2  : return "two"          ;
            case 3  : /* FALL-THROUGH */
            case 4  : return "three-or-four";
            default : return "unknown"      ;
        } // switch
    }

    // Declaration alignment test
    public void declarations()
    {
        int     x      = 1;
        long    bigNum = 123456L;
        String  label  = "hello";
        boolean flag   = true;
        double  ratio  = 3.14;
    }

    // Modifier ordering: should be public static final, not static public final
    public static final int WRONG_ORDER = 42;

    // Nested class
    public static class Inner {

        private int value;
        public Inner(int value) { this.value = value; }
        public int getValue() { return value; }

    } // class Inner

    // Interface
    public interface Processor {

        void process(String input);
        default String preprocess(String input)
        {
            return input.trim();
        }

    } // interface Processor

    // Enum
    public enum Status {

        ACTIVE, INACTIVE, PENDING;

        public boolean isTerminal()
        {
            return this == INACTIVE;
        }

    } // enum Status

    // Try-catch-finally
    public String readSafely(String key, Map<String, String> map)
    {
        try {
            return map.get(key).trim();
        }
        catch(NullPointerException e) {
            return "";
        }
        finally {
            System.out.println("done");
        }
    }

    // Ternary and complex expressions
    public int clamp(int val, int lo, int hi)
    {
        return val < lo ? lo : val > hi ? hi : val;
    }

    // Anonymous class
    public Runnable makeRunnable()
    {
        return new Runnable() {
            public void run()
            {
                System.out.println("running");
            }
        }; // class
    }

    // Lambda (Java 8)
    public Runnable makeLambda()
    {
        return () -> System.out.println("lambda");
    }

    @Override
    public void run()
    {
        process( new ArrayList<String>() );
    }

} // class CoreExample
