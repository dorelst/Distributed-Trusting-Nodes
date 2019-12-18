import java.util.Random;

public class TestRandInt {
    public static void main(String[] args) {
        int a, b=0, sumLower =0, sumHigher = 0;

        Random rand = new Random();
        for (int i = 0; i<100; i++ ) {
           a = rand.nextInt(100)+1;
           System.out.println("a = "+a);
           b = (rand.nextInt(100)+1)<25 ? 1 : -1;
           System.out.println("b = "+b);
           if (b == 1) {
               sumLower++;
           }

           if (b==-1) {
               sumHigher++;
           }
        }
        System.out.println("SumLower = "+sumLower+" SumHigher = "+sumHigher);
    }
}
