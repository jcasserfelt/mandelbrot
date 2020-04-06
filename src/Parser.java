import java.util.ArrayList;

public class Parser {
    private double min_c_re;
    private double min_c_im;
    private double max_c_re;
    private double max_c_im;
    private int inf_n;
    private int x;
    private int y;
    private int divider;

    //  "-1 -1 1 1 1024 1000 1000 2 localhost:8001 localhost:8002"

    public static String[] parse(String input) {
        String[] parsedString;
        parsedString = input.split(" ");
        return parsedString;

    }

    public static ArrayList<String> separateServers(String[] inputArray) {
        ArrayList<String> serverList = new ArrayList<>();
        if (inputArray.length > 8) {
            for (int i = 8; i < inputArray.length; i++) {
                serverList.add(inputArray[i]);
            }
        }
        return serverList;
    }

    public String validateMandelbrotInput(String[] input) throws NumberFormatException {


        this.min_c_re = Double.parseDouble(input[0]);
        this.min_c_im = Double.parseDouble(input[1]);
        this.max_c_re = Double.parseDouble(input[2]);
        this.max_c_im = Double.parseDouble(input[3]);
        this.inf_n = Integer.parseInt(input[4]);
        this.x = Integer.parseInt(input[5]);
        this.y = Integer.parseInt(input[6]);
        this.divider = Integer.parseInt(input[7]);

        double xStepSize = Math.floor(x / divider);
        double yStepSize = Math.floor(y / divider);

        String temp = String.format("%f %f %f %f %d %d %d %d", min_c_re, min_c_im, max_c_re, max_c_im, inf_n, x, y, divider);
        return temp;

    }


}
