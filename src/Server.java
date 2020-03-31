import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private double min_c_re;                    // min real value
    private double min_c_im;                    // min imaginary value
    private double max_c_re;                    // max real value
    private double max_c_im;                    // max imaginary value
    private int inf_n;                          // iteration limit
    private double xStepSize;                   // width resolution sub-area
    private double yStepSize;                   // height resolution of sub-area

    private String stage;
    private byte[] subResultArray;
    private ArrayList<String> workPackageList = new ArrayList<>();
    private ArrayList<Coordinate> subAreaCoordinates = new ArrayList<>();
    private ArrayList<byte[]> subResultList = new ArrayList<>();
    private ServerSocket listener;
    private Socket socket;
    private BufferedReader bufferedReaderInput;
    private String[] tempWorkPak;

    Server(int port) throws IOException {
        // start up a listener
        listener = new ServerSocket(port);

        // will go on for a while
        while (true) {
            // listen for incoming calls
            socket = listener.accept();

            // use connected socket to create BufferedReader,
            // is used to receive calculation boundaries and statuses
            bufferedReaderInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // receives Strings with information on boundaries etc for
            // calculations
            receiveWork();

            // look at each received string, parse it to varables,
            // performe calculations and store results in byte-arrays
            handleWorkPackage();

            // send back back each byte-array
            sendResults();
        }
    }


    /**
     * Sends byte-arrays containing results from calculations.
     * Each byte-array represents a sub-area in user defined area.
     */
    private void sendResults() throws IOException {
        stage = bufferedReaderInput.readLine();

        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        for (byte[] b : subResultList) {
            dOut.writeInt(b.length);            // write length of the message
            dOut.write(b);                       // write the message
        }
        stage = bufferedReaderInput.readLine();
    }

    /**
     * Alot going on here, could really use some restructuring
     * <p>
     * Iterates over the strings received from Client.
     * <p>
     * For each iteration, string is parsed into variables.
     * variable-values is used to define a set of coordinates.
     * <p>
     * The set of coordinates is being iterated over
     * to calculate result. each coordinate has x and y values for
     * mandelbrot iteration count.
     * <p>
     * For each coordinate, an iteration number is produced. That number
     * gets converted to signed byte and stored in byte-array.
     * <p>
     * One string represents one sub-area, and one byte-array.
     */
    public void handleWorkPackage() {
        int counter = 0;
        for (String s : workPackageList) {
            appendVariables(s);
            getSubAreaCoordinates(); // tar bara en subArea
            calculateSubArea();
        }
    }

    /**
     * Receives strings and store them to a list.
     */
    public void receiveWork() throws IOException {
        // this method could use a little polish..

        String message = bufferedReaderInput.readLine();
        while (!message.equals("stage_WorkPackages_sent")) {

            message = bufferedReaderInput.readLine();
            if (message.equals("stage_WorkPackages_sent")) break;

            workPackageList.add(message);
            message = bufferedReaderInput.readLine();
        }
    }


    /**
     * This need to get more OOP. Needs to throw exception
     * Parses values from String array into instance variables.
     */
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

    public static void main(String[] args) throws IOException {
        Server server1 = new Server(8001);
    }


    /**
     * Generates instances of POJO class Coordinate, and plenty of it.
     * The purpose is to keep track of correlation of mandelbrot input
     * parameters (the x and y values), and where that result is being
     * stored in the result array, and finally where the pixel will be located
     * in the PGM image.
     * <p>
     * The coordinates is being generated from the top-left corner of the
     * sub-area, then goes one step to the right until end of row.
     */
    public void getSubAreaCoordinates() {
        subAreaCoordinates = null;
        subAreaCoordinates = new ArrayList<>();
        double xInterval = Math.abs(max_c_re - min_c_re);
        double yInterval = Math.abs(max_c_im - min_c_im);

        double tempX = min_c_re;
        double tempY = max_c_im;

        double xAdd;
        double ySub;

        int x = (int) xStepSize;
        int y = (int) yStepSize;


        // create x*y coordinates
        for (int i = 0; i < y; i++) {
            if (i != 0) {
                ySub = (yInterval / (x - 1));
                tempY = tempY - ySub;
            }
            tempX = min_c_re;           // restart x value for the next row
            for (int j = 0; j < x; j++) {
                subAreaCoordinates.add(new Coordinate(tempX, tempY));
                xAdd = (xInterval / (y - 1));
                tempX = tempX + xAdd;
            }
        }
    }


    /**
     * Goes through all coordinates in current sub-area and use its
     * values to calculate corresponding amount mandelbrot iterations
     * Uses convertToPGMRangeByte() to convert integer to signed byte.
     */
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

    /**
     * Converts input value to a value ranged 0-255, then
     * converted to signed byte.
     * <p>
     * The purpose of this is that the PGM file format used
     * has a range from 0-255. Therefore the amount of mandelbrot
     * iterations must be scaled down to the PGM range proportionally.
     * <p>
     * And since the information can be stored in a single byte,
     * that is the preferred format for transfer over networks.
     */
    public byte convertToPGMRangeByte(double input, double inf_n) {
        int pgmMaxValue = 255;
        if (input == 0) return 0;
        double coefficient = pgmMaxValue / inf_n;
        byte result = (byte) (Math.floor(input * coefficient) + 1);
        return result;
    }


    /**
     * Calculates how many times is takes for a certain point on
     * the real-imaginary plane to grow out of a certain value (2).
     *
     * Returns the amount of iterations, or defined max-iteration
     */
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
                return i;
            }
        }
        if (i == ITERATIONS) {

            return ITERATIONS;
        }
        return ITERATIONS;
    }
}