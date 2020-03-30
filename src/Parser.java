import java.util.ArrayList;

public class Parser {
    private int min_c_re;
    private int min_c_im;
    private int max_c_re;
    private int max_c_im;
    private int x;
    private int y;
    private int inf_n;

    public static String[] parse(String input){
        String[] parsedString;
        parsedString = input.split(" ");
        return parsedString;

    }

    public static ArrayList<String> separateServers(String[] inputArray){
        ArrayList<String> serverList = new ArrayList<>();
        if (inputArray.length > 8){
            for (int i = 8; i < inputArray.length; i++) {
                serverList.add(inputArray[i]);
            }
        }
        return serverList;
    }
}
