package junk;

public class TestByteConverter {

    public static final int ITTE = 1024;

    public static void main(String[] args) {

        int counter;

        for (int i = 0; i < ITTE; i++) {
            System.out.println(i + ": " + convertToPGMRangeByte1(i, ITTE) + " " + (byte) convertToPGMRangeByte1(i, ITTE) + " " + (((int) convertToPGMRangeByte1(i, ITTE)) & 0xFF));
            System.out.println( convertToPGMRangeByte(i, ITTE));
        }
    }

    // it works!
    public static byte convertToPGMRangeByte(double input, double inf_n) {
        int pgmMaxValue = 255;
        if (input == 0) return 0;
        double coefficient = pgmMaxValue / inf_n;
        byte result = (byte) (Math.floor(input * coefficient) + 1);
        return result;
    }

    public static int convertToPGMRangeByte1(double input, int inf_n) {
        if (input == 0) return 0;
        double cofe = (float) 255 / (float) inf_n;
        int result = (int) Math.floor(input * cofe) + 1;
        return result;
    }
}
