import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server2 {
    private Parser parser;

    private double min_c_re;
    private double min_c_im;
    private double max_c_re;
    private double max_c_im;
    private int x;
    private int y;
    private int inf_n;
    private int devider;
    byte[] resultArray;
    int[] subResultArray1D;
    int[][] subResultArray2D;
    double xStepSize;
    double yStepSize;
    String[] inputArray;

    byte[] returnArray;
    ArrayList<String> workPackageList = new ArrayList<>();
    ServerSocket listener;
    Socket socket;
    ObjectOutputStream outputObject;
    BufferedReader input;
    String[] tempWorkPak;

    Server2(int port) throws IOException {
        // todo kanske kora parsning metoder i denna klass?
        listener = new ServerSocket(port);
        socket = listener.accept();
        System.out.println("accept körd");
        outputObject = new ObjectOutputStream(socket.getOutputStream());
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        receiveWork();
        System.out.println("stringList size: " + workPackageList.size());
        handleWorkPackage();
//        final BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
//        image.getRaster().setDataElements(0, 0, 200, 200, subResultArray1D);

//        SwingUtilities.invokeLater(new
//
//                                           Runnable() {
//                                               @Override
//                                               public void run() {
//                                                   JFrame frame = new JFrame(getClass().getSimpleName());
//                                                   frame.add(new JLabel(new ImageIcon(image)));
//                                                   frame.setSize(400, 400);
//                                                   frame.setResizable(true);
//                                                   frame.pack();
//                                                   frame.setLocationRelativeTo(null);
//                                                   frame.setVisible(true);
//
//                                               }
//                                           });

    }

    public void handleWorkPackage() {
        for (String s : workPackageList) {
            appendVariables2(s);
            System.out.println("printat from privat variabel: " + this.min_c_re);
            // todo här ska beräkning ske i varje iteration
            populate2dArrayReverse();

        }
    }

    public void receiveWork() throws IOException {
        String userInput;
        while ((userInput = input.readLine()) != null) {
            System.out.println(userInput);
            workPackageList.add(userInput);
        }
    }

    public void appendVariables2(String s) {
        this.tempWorkPak = s.split(" ");
        for (int i = 0; i < tempWorkPak.length; i++) {
            this.min_c_re = Double.parseDouble(tempWorkPak[0].replace(",", "."));
            this.max_c_im = Double.parseDouble(tempWorkPak[1].replace(",", "."));
            this.max_c_re = Double.parseDouble(tempWorkPak[2].replace(",", "."));
            this.min_c_im = Double.parseDouble(tempWorkPak[3].replace(",", "."));
            this.inf_n = Integer.parseInt(tempWorkPak[4]);
            this.xStepSize = Double.parseDouble(tempWorkPak[5].replace(",", "."));
            this.yStepSize = Double.parseDouble(tempWorkPak[6].replace(",", "."));
        }
    }

    public static void main(String[] args) throws IOException {
        Server server1 = new Server(8002);
    }

    public void mandelCalc(ArrayList<String> input) {
        int subArrayLenght = (int) (xStepSize * yStepSize);
        byte[] temp = new byte[subArrayLenght];
        int[] intArray = new int[subArrayLenght];
    }

    public int countIterations(double x, double y, int inf_n) {
        // The Mandelbrot set is represented by coloring
        // each point (x,y) according to the number of
        // iterations it takes before the while loop in
        // this method ends.  For points that are actually
        // in the Mandelbrot set, or very close to it, the
        // count will reach the maximum value, 80.  These
        // points will be colored purple.  All other colors
        // represent points that are definitely NOT in the set.
        int count = 0;
        double zx = x;
        double zy = y;
        while (count < inf_n && Math.abs(x) < 100 && Math.abs(zy) < 100) {
            double new_zx = zx * zx - zy * zy + x;
            zy = 2 * zx * zy + y;
            zx = new_zx;
            count++;
        }
        return count;
    }

    Coordinate[][] twoDInputArray;

    public void populate2dArray() {

        double xInterval = Math.abs(max_c_re - min_c_re);
        double yInterval = Math.abs(max_c_im - min_c_im);

        double tempX = min_c_re;
        double tempY = max_c_im;

        double xAdd;
        double ySub;

        int x = (int) xStepSize;
        int y = (int) yStepSize;
        subResultArray1D = new int[x * y];
        subResultArray2D = new int[x][y];

        twoDInputArray = new Coordinate[x][y];
        for (int i = 0; i < x; i++) {
            if (i != 0) {
                xAdd = (xInterval / (x - 1));
                tempX = tempX + xAdd;
            }
            tempY = max_c_im;
            for (int j = 0; j < y; j++) {

                ySub = (yInterval / (y - 1));
                tempY = tempY - ySub;
            }
        }
    }

    public void populate2dArrayReverse() {

        double xInterval = Math.abs(max_c_re - min_c_re);
        double yInterval = Math.abs(max_c_im - min_c_im);

        double tempX = min_c_re;
        double tempY = max_c_im;

        double xAdd;
        double ySub;

        int x = (int) xStepSize;
        int y = (int) yStepSize;
        subResultArray1D = new int[x * y];
        subResultArray2D = new int[x][y];

        twoDInputArray = new Coordinate[x][y];
        int counter = 0;
        for (int i = 0; i < y; i++) {
            if (i != 0) {
                ySub = (yInterval / (x - 1));
                tempY = tempY - ySub;
            }
            tempX = min_c_re; // restart x value for the next row
            for (int j = 0; j < x; j++) {
                twoDInputArray[i][j] = new Coordinate(tempX, tempY);
                subResultArray1D[counter] = calculatePoint(tempX, tempY);
                xAdd = (xInterval / (y - 1));
                tempX = tempX + xAdd;
                counter++;

            }
        }
    }

    public int calculatePoint(double x, double y) {
        int ITERATIONS = this.inf_n;

        double cx = x;
        double cy = y;

        int i = 0;
        for (i = 0; i < ITERATIONS; i++) {
            double nx = x * x - y * y + cx;
            double ny = 2 * x * y + cy;
            x = nx;
            y = ny;

            if (x * x + y * y > 2) {
//                System.out.println("unstable: " + i);
//                System.out.println("unstable: " + (byte) i + " (castad)");
                return i;

            }
        }
        if (i == ITERATIONS) {
//            System.out.println("stable: " + i);
//            System.out.println("stable: " + (byte) i + "(castad)");
            return ITERATIONS;
//            return (byte) 255;

        }

        return ITERATIONS;
    }
}
