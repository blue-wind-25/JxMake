// javac main.java
// find . -name '*.class' | xargs /bin/rm -vf

import java.util.Arrays;
import java.util.function.*;

import lib.*;

public class main {

    public static void main(String[] args)
    {
        final l1    _l1    = new l1();
        final l2    _l2    = new l2();

        final lib1a _lib1a = new lib1a();
        final lib2  _lib2  = new lib2();

        System.out.printf("\nHELLO WORLD from Java!\n\n");

        System.out.printf("jsp_test.Value_X0 = '%s'\n", jsp_test.Value_X0);
        System.out.printf("jsp_test.Value_X1 = '%s'\n", jsp_test.Value_X1);

        System.out.printf("jsp_test.Value_0b = '%s'\n", jsp_test.Value_0b);
        System.out.printf("jsp_test.Value_0s = '%s'\n", jsp_test.Value_0s);
        System.out.printf("jsp_test.Value_1t = '%s'\n", jsp_test.Value_1t);
        System.out.printf("jsp_test.Value_1f = '%s'\n", jsp_test.Value_1f);
        System.out.printf("jsp_test.Value012 = '%s'\n", jsp_test.Value012);

        System.out.println();
    }

}
