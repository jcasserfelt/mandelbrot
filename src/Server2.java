import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server2 {
    private String stage;
    private double min_c_re;
    private double min_c_im;
    private double max_c_re;
    private double max_c_im;
    private int inf_n;
    private byte[] subResultArray;
    //    private int[] subResultArray1D;
    private double xStepSize;
    private double yStepSize;
    private ArrayList<String> workPackageList = new ArrayList<>();
    private ArrayList<Coordinate> subAreaCoordinates = new ArrayList<>();
    private ArrayList<byte[]> subResultList = new ArrayList<>();
    private ServerSocket listener;
    private Socket socket;
    private BufferedReader bufferedReaderInput;
    private String[] tempWorkPak;

    public static void main(String[] args) throws IOException {
        Server2 server2 = new Server2(8002);
    }

    Server2(int port) throws IOException {
        listener = new ServerSocket(port);
        while (true) {
            socket = listener.accept();
            // todo put in separate method, try with resouces and catch IOException
            bufferedReaderInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            receiveWork();
            handleWorkPackage();
            sendResults();
        }
    }

    private void sendResults() throws IOException {
        stage = bufferedReaderInput.readLine();
        System.out.println(stage);

        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        for (byte[] b : subResultList) {
            dOut.writeInt(b.length);            // write length of the message
            System.out.println("server1 has sent lenght");
            dOut.write(b);                       // write the message
        }
        stage = bufferedReaderInput.readLine();
        System.out.println(stage);
    }
    // todo här ska all magic ske

    public void handleWorkPackage() {
        int counter = 0;
        for (String s : workPackageList) {
            appendVariables(s);
            System.out.println("printat from privat variabel: " + this.min_c_re);
            getSubAreaCoordinates(); // tar bara en subArea
            calculateSubArea();
        }
    }
    //todo make it dynamic!!!

    public void receiveWork() throws IOException {


        String message = bufferedReaderInput.readLine();
//        int divider = Integer.parseInt(message);

        while (!message.equals("stage_WorkPackages_sent")) {

            message = bufferedReaderInput.readLine();
            if (message.equals("stage_WorkPackages_sent")) break;


//            String userInput;
//            userInput = bufferedReaderInput.readLine();
//            System.out.println(message);
            workPackageList.add(message);
            message = bufferedReaderInput.readLine();
        }

//        System.out.println(stage);
    }

    public void appendVariables(String s) {
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

    public void getSubAreaCoordinates() {
        subAreaCoordinates = null;  // remove asap
        subAreaCoordinates = new ArrayList<>();
        double xInterval = Math.abs(max_c_re - min_c_re);
        double yInterval = Math.abs(max_c_im - min_c_im);

        double tempX = min_c_re;
        double tempY = max_c_im;

        double xAdd;
        double ySub;

        int x = (int) xStepSize;
        int y = (int) yStepSize;
//        subResultArray1D = new int[x * y];
        int counter = 0;
        for (int i = 0; i < y; i++) {
            if (i != 0) {
                ySub = (yInterval / (x - 1));
                tempY = tempY - ySub;
            }
            tempX = min_c_re; // restart x value for the next row
            for (int j = 0; j < x; j++) {
                subAreaCoordinates.add(new Coordinate(tempX, tempY));
//                subResultArray1D[counter] = calculatePoint(tempX, tempY); // remove asap
                xAdd = (xInterval / (y - 1));
                tempX = tempX + xAdd;
                counter++;
            }
        }
    }

    public void calculateSubArea() {
        int arraySize = (int) xStepSize * (int) yStepSize;
        subResultArray = new byte[arraySize];
        int counter = 0;
        try {
            for (Coordinate tempCoordinate : this.subAreaCoordinates) {
                subResultArray[counter] = this.convertToPGMRangeByte(calculatePoint(tempCoordinate.x, tempCoordinate.y), inf_n);
                counter++;
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Index vid outofbounce: " + counter);
            e.printStackTrace();
        }
        subResultList.add(subResultArray);
    }

    // convert no of iterations to signed byte range
    public byte convertToPGMRangeByte(double input, double inf_n) {
        int pgmMaxValue = 255;
        if (input == 0) return 0;
        double coefficient = pgmMaxValue / inf_n;
        byte result = (byte) (Math.floor(input * coefficient) + 1);
        return result;
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
//                System.out.println("ostabil byteConverter: " + convertToPGMRangeByte(i)); // todo se hit
                return i;
            }
        }
        if (i == ITERATIONS) {
//            System.out.println("stable: " + i);
//            System.out.println("stable: " + (byte) i + "(castad)");
//            System.out.println("ostabil byteConverter: " + convertToByte(ITERATIONS));
            return ITERATIONS;
//            return (byte) 255;
        }
        return ITERATIONS;
    }
}