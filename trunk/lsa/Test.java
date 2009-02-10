import java.util.concurrent.atomic.*;

public class Test {

    public static void main(String[] args) {
	long sum = 0;
	for (int i = 0; i < 10; ++i) {
	    long cur = System.currentTimeMillis();
	    int[] arr = new int[1<< 25];
	    sum += System.currentTimeMillis() - cur;
	}
	System.out.println("array creation takes: " + (sum / 1000d));

	sum = 0;
	for (int i = 0; i < 10; ++i) {
	    long cur = System.currentTimeMillis();
	    AtomicIntegerArray arr = new AtomicIntegerArray(1 << 25
);
	    sum += System.currentTimeMillis() - cur;
	}
	System.out.println("atomic array creation takes: " + (sum / 1000d));

	AtomicIntegerArray arr2 = new AtomicIntegerArray(1);
	arr2.set(10, 1);
	System.out.println(arr2.length());

    }

}