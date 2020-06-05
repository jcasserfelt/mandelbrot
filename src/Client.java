import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.InputMismatchException;

public class Client {
    private double min_c_re;              // min real value
    private double min_c_im;              // min imaginary value
    private double max_c_re;              // max real value
    private double max_c_im;              // max imaginary value
    private int x;                        // width resolution of total area
    private int y;                        // height resolution of total area
    private int inf_n;                    // iteration limit
    private int divider;                  // number of height and width divisions of total area
    private double xStepSize;             // width resolution of total area
    private double yStepSize;             // height resolution of total area

    private ArrayList<byte[]> subArrayList = new ArrayList<>();
    private ArrayList<PrintWriter> connectionOutList = new ArrayList<>();

    // example input for program arguments:
    // -1 -1 1 1 256 1200 1200 2 localhost:8001 localhost:8002

    public static void main(String[] args) {

        Client client = new Client();
        try {
            String input = client.validateMandelbrotInput(args);
            String[] serverArray = client.getServerNames(args);
            client.startLogic(input, serverArray);

        } catch (NumberFormatException e) {
            System.out.println("Wrong input format");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startLogic(String input, String[] serverArray) throws IOException {
        String[] workpacks = prepareWorkload2(this.min_c_re, this.min_c_im, this.max_c_re, this.max_c_im, this.inf_n, this.x, this.y, this.divider);
        Socket[] sockets = createSockets2(serverArray);
        PrintWriter[] outStreams2 = createOutStreams2(sockets);
        DataInputStream[] inputStreams = createInStreams2(sockets);
        divideWorkBetweenServers2(sockets, outStreams2, workpacks);
        System.out.println("Waiting for results..");
        byte[][] subResults = receiveResult2(outStreams2, inputStreams, workpacks.length);
        System.out.println("Results Received");
        createSubResultImages2(subResults, this.xStepSize, this.yStepSize);
    }

    Client() {
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

        this.xStepSize = Math.floor((double) x / divider);
        this.yStepSize = Math.floor((double) y / divider);

        String temp = String.format("%f %f %f %f %d %d %d %d", min_c_re, min_c_im, max_c_re, max_c_im, inf_n, x, y, divider);
        return temp;
    }

    public String[] getServerNames(String[] input) {
        int numberOfServers = input.length - 8;
        String[] serverArray = new String[numberOfServers];

        int counter = 0;
        for (int i = 8; i < input.length; i++) {
            serverArray[counter] = input[i];
            counter++;
        }
        return serverArray;
    }

    /**
     * Iterates over a list of strings, hopefully containing something along the lines of "localhost:8001"
     * Then splits the string at ":" to use for create new Socket
     * <p>
     * Amount of sockets will be the same as parameter-list length
     */
    public Socket[] createSockets2(String[] serversNames) throws IOException {
        Socket[] sockets = new Socket[serversNames.length];
        for (int i = 0; i < serversNames.length; i++) {
            String[] temp = serversNames[i].split(":");
            sockets[i] = new Socket(temp[0], Integer.parseInt(temp[1]));
        }
        return sockets;
    }

    /**
     * Iterates over a list of sockets, creating a new PrintWriter for each socket.
     * <p>
     * Amount of PrintWriters will be the same as parameter-list length
     */
    public PrintWriter[] createOutStreams2(Socket[] sockets) throws IOException {
        PrintWriter[] printWriters = new PrintWriter[sockets.length];
        for (int i = 0; i < sockets.length; i++) {
            printWriters[i] = new PrintWriter(sockets[i].getOutputStream(), true);
        }
        return printWriters;
    }

    /**
     * Dividing specified work load into specified amount
     * of sub packages. Using defined outer boundaries and
     * amount of sub-areas, one can define the sub-areas
     * boundaries.
     * <p>
     * Each sub-area is described in a string and stored
     * in a list of strings.
     */
    public String[] prepareWorkload2(double min_c_re, double min_c_im, double max_c_re, double max_c_im, int inf_n, double x, double y, int divider) {
        String[] workPacksArray = new String[divider * divider];
        double minX = min_c_re;
        double minY = min_c_im;
        double maxX = max_c_re;
        double maxY = max_c_im;
        String temp = "";
        double xStepSize = Math.floor(x / (double) divider);
        double yStepSize = Math.floor(y / (double) divider);

        double xInterval = maxX - minX;     // 2
        double yInterval = maxY - minY;     // 2

        double yDiff = yInterval / divider;
        double xDiff = xInterval / divider;

        int counter = 0;
        for (int i = 0; i < divider; i++) {
            if (i != 0) {
                maxY = maxY - yDiff;
            }
            minY = maxY - yDiff;
            for (int j = 0; j < divider; j++) {
                if (j != 0) {
                    minX = minX + (xDiff);
                }
                maxX = minX + xDiff;

                temp = String.format("%f %f %f %f %d %f %f", minX, minY, maxX, maxY, inf_n, xStepSize, yStepSize);
                workPacksArray[counter] = temp;
                counter++;
            }
            // reset x-values
            minX = min_c_re;
            maxX = max_c_re;
        }
        return workPacksArray;
    }

    /**
     * Iterates over a list of sockets, creating a new
     * DataInputStream for each socket.
     * <p>
     * Amount of DataInputStreams will be the same as
     * parameter-list length
     */
    public DataInputStream[] createInStreams2(Socket[] sockets) throws IOException {
        DataInputStream[] dataInputStreams = new DataInputStream[sockets.length];
        for (int i = 0; i < sockets.length; i++) {
            dataInputStreams[i] = new DataInputStream(sockets[i].getInputStream());
        }
        return dataInputStreams;
    }


    /**
     * Writes and listens according to Server class
     * method sendResults()
     * <p>
     * Amount of received byte-arrays will be the
     * same as user defined division*division
     * ie the amount of workpackages
     * <p>
     * Store byte-arrays in a list
     */
    // todo consider only passing in socket rather than streams
    public byte[][] receiveResult2(PrintWriter[] outputSteams, DataInputStream[] inputStreams, int numberOfWorkPacks) throws IOException {
        byte[][] subResults = new byte[divider * divider][];

        for (int i = 0; i < outputSteams.length; i++) {
            outputSteams[i].println("stage_ready_to_receive");
        }

        // receive result1 from server1, result2 from server2, ...
        int counter = 0;
        for (int i = 0; i < numberOfWorkPacks; i++) {
            int length = inputStreams[counter].readInt();
            if (length > 0) {
                byte[] result = new byte[length];
                inputStreams[counter].readFully(result, 0, result.length);
                subResults[i] = result;
            }
            counter++;
            if (counter == inputStreams.length) counter = 0;
        }

        for (PrintWriter p : connectionOutList) {
            p.println("stage_results_received");
        }
        return subResults;
    }


    /**
     * Sends out workload to user defined server(s)
     * Workload is sent as a prepared string which describes
     * boundaries.
     * <p>
     * Also send status to receiving end which is
     * Server class method receiveWork()
     */
    public void divideWorkBetweenServers2(Socket[] sockets, PrintWriter[] outConnections, String[] workPacks) {

        int amountOfServers = sockets.length;            //2
        int workPackCounter = 0;
        for (int i = 0; i < workPacks.length; i++) {
            outConnections[workPackCounter].println(workPacks[i]);
            outConnections[workPackCounter].println("continue");
            workPackCounter++;
            if (workPackCounter == amountOfServers) workPackCounter = 0;
        }

        for (int i = 0; i < outConnections.length; i++) {
            outConnections[i].println("stage_WorkPackages_sent");
        }
    }


    /**
     * Creates PGM image from each workpack-result received.
     * The results are byte-arrays with signed bytes.
     * To convert signed byte value to integer, use bit operation: & 0xff.
     * ie (((byte)-127) & 0xff) == 255
     * <p>
     * Image is named dynamically (nice) and stored in project folder for now.
     */
    // todo implement try with resources riiight here,
    public void createSubResultImages2(byte[][] subResults, double xStepSize, double yStepSize) {
        // check if data matches intended image-size
        if (subResults[0].length != xStepSize * yStepSize) throw new InputMismatchException();

        for (int i = 0; i < subResults.length; i++) {
            String filename = "subResult" + (i + 1) + ".pgm";
            File file = new File("Results" + File.separator + filename);
            try (PrintWriter pw = new PrintWriter(file)) {

                for (int j = 0; j < subResults.length; j++) {
                    int maxvall = 255;
                    int width = (int) xStepSize;
                    int height = (int) yStepSize;

                    // magic number, width, height, and maxval
                    pw.println("P2");
                    pw.println(width + " " + height);
                    pw.println(maxvall);

                    // print out the data, limiting the line lengths to 70 characters
                    int lineLength = 0;

                    for (int k = 0; k < subResults[j].length; k++) {
                        int value = subResults[i][k] & 0xff;

                        String stringValue = "" + value;
                        int currentLength = stringValue.length() + 1;
                        if (currentLength + lineLength > 70) {
                            pw.println();
                            lineLength = 0;
                        }
                        lineLength += currentLength;
                        pw.print(value + " ");
                    }
                    pw.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    // todo: fix these
    // I havnt got the functionality to combine multiple
    // sub-results and render them in to a combined image.

    // See Combined Result Render Fails in project folder.
    // Result6.pgm is the result when just iterate over
    // all received sub-results in reveived order.

    // I have tried to go through each row this.divider times
    // but that created carnage like result4 and 5 in fail-folder.

    // does not work
    public void createFile(String filename) throws FileNotFoundException {
        int maxvall = 255;

        PrintWriter pw = new PrintWriter(filename);
        int width = this.x;
        int height = this.y;

        // magic number, width, height, and maxval
        pw.println("P2");
        pw.println(width + " " + height);
        pw.println(maxvall);

        // print out the data, limiting the line lengths to 70 characters
        int lineLength = 0;


        // re-arrange pixels to fit combined result image
        int imagesize = this.subArrayList.size() * subArrayList.get(0).length;
        byte[] testArray = new byte[imagesize];
        int testCounter = 0;
        int indexAdjuster = 0;
        ArrayList<ArrayList<byte[]>> subAreaRows = new ArrayList<>();
        ArrayList<byte[]> tempList = new ArrayList<>();
        for (int i = 0; i < divider; i++) {

            for (int j = 0; j < divider; j++) {
                tempList.add(subArrayList.get(j + indexAdjuster));
            }
            subAreaRows.add(tempList);
            indexAdjuster = +divider;
            tempList = new ArrayList<>();
        }

        for (ArrayList<byte[]> list : subAreaRows) {        // 1 och 2
            for (int i = 1; i <= yStepSize; i++) {
                for (int j = 0; j < divider; j++) {
                    for (int k = 0; k < xStepSize; k++) {
                        testArray[testCounter] = list.get(j)[k * i];
                        testCounter++;
                    }

                }

            }


//            int rowsOfSubAreas = devider;
//            int columnsOfSubArea = devider;
//            int rowsPerSubArea = (int) yStepSize;
//            int columnsPerSubArea = (int) xStepSize;
//            byte tempByte;
//            int indexAdjuster = 1;
//            for (int i = 1; i <= rowsOfSubAreas; i++) {                         // 2
//                for (int j = 1; j <= rowsPerSubArea; j++) {                     // 400
//                    for (int k = 0; k < columnsOfSubArea; k++) {               // 2
//                        for (int l = 0; l < columnsPerSubArea; l++) {           // 400
//                            if (i == 1) {
//                                tempByte = subArrayList.get((k * i))[l * j];
//                                testArray[testCounter] = subArrayList.get(k * i)[l * j];
//                            } else if (i == 2) {
//                                tempByte = subArrayList.get((k + i))[l * j];
//                                testArray[testCounter] = subArrayList.get(k * i)[l * j];
//                            }
//                            testCounter++;
//                        }
//                    }
//                }
//                indexAdjuster++;
//            }


            try {
                for (int i = 0; i < testArray.length; i++) {
                    int value = testArray[i] & 0xff;

                    String stringValue = "" + value;
                    int currentLength = stringValue.length() + 1;
                    if (currentLength + lineLength > 70) {
                        pw.println();
                        lineLength = 0;
                    }
                    lineLength += currentLength;
                    pw.print(value + " ");
                }

            } catch (IndexOutOfBoundsException e) {
                System.out.println("out of bounce vid index: " + testCounter);
            }
            pw.close();
        }
    }

    // does not work
    public void createFile2(String filename) throws FileNotFoundException {
        int maxvall = 255;

        PrintWriter pw = new PrintWriter(filename);
        int width = this.x;
        int height = this.y;

        // magic number, width, height, and maxval
        pw.println("P2");
        pw.println(width + " " + height);
        pw.println(maxvall);

        // print out the data, limiting the line lengths to 70 characters
        int lineLength = 0;

        int imagesize = this.subArrayList.size() * subArrayList.get(0).length;

        for (byte[] b : subArrayList) {
            for (int i = 0; i < b.length; i++) {
                int value = b[i] & 0xff;

                String stringValue = "" + value;
                int currentLength = stringValue.length() + 1;
                if (currentLength + lineLength > 70) {
                    pw.println();
                    lineLength = 0;
                }
                lineLength += currentLength;
                pw.print(value + " ");
            }
        }
        pw.close();
    }
}
