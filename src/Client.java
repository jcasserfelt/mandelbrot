import java.io.*;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.ArrayList;


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
    private ArrayList<String> serverList;
    private ArrayList<Socket> socketList = new ArrayList<>();
    private ArrayList<byte[]> subArrayList = new ArrayList<>();
    private ArrayList<String> workPackages = new ArrayList<>();
    private ArrayList<PrintWriter> connectionOutList = new ArrayList<>();
    private ArrayList<DataInputStream> dataInputList = new ArrayList<>();
    private String[] inputArray;


    // Adjust input parameters here!!
    // See branch system-args-input for a very unrefined implementation.
    private String exampleInput = "-0.76 0.04 -0.75 0.05 1024 2400 2400 2 localhost:8001 localhost:8002";
    Client() {

        // splits user input into separate strings and append to instance variable respectively
        inputArray = Parser.parse(exampleInput);
        serverList = Parser.separateServers(inputArray);
        this.appendVariables();

        // divide work load into pieces. ie 2*2, 3*3, ... ,  sub areas in the user defined area.
        this.prepareWorkload();

        try {
            // when work load is prepared, connect to server(s)
            // create socket(s) from list of strings like "localhost:8001"
            socketList = createSockets(serverList);
            connectionOutList = createOutStreams(socketList);
            dataInputList = createInStreams(socketList);

            // iterate list of workpack(s), and send them to to server(s)
            // workpack1 -> server 1, workpack2 -> server2 ...
            divideWorkBetweenServers();
            System.out.println("Waiting for results");

            // wait to receive result(s) from server(s), same order as sent
            // each workpack comes back as byte-array.
            // each byte-array is stored in a list.
            receiveResult();
            System.out.println("Results Received");

            // use the received byte-arrays to create PGM images
            // each image corresponds to a workpack
            createSubResultImages();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // all the logic happens in the constructor. This is probably the highest priority to get changed going forward.
        Client client = new Client();
    }


    /**
     * Iterates over a list of strings, hopefully containing something along the lines of "localhost:8001"
     * Then splits the string at ":" to use for create new Socket
     * <p>
     * Amount of sockets will be the same as parameter-list length
     */
    public ArrayList<Socket> createSockets(ArrayList<String> serverList) throws IOException {
        ArrayList<Socket> sockets = new ArrayList<>();
        for (String s : serverList) {
            String[] temp = s.split(":");
            sockets.add(new Socket(temp[0], Integer.parseInt(temp[1])));
        }
        return sockets;

    }


    /**
     * Iterates over a list of sockets, creating a new PrintWriter for each socket.
     * <p>
     * Amount of PrintWriters will be the same as parameter-list length
     */
    public ArrayList<PrintWriter> createOutStreams(ArrayList<Socket> socketList) throws IOException {
        ArrayList<PrintWriter> outStreams = new ArrayList<>();
        for (int i = 0; i < socketList.size(); i++) {
            outStreams.add((new PrintWriter(socketList.get(i).getOutputStream(), true)));
        }
        return outStreams;
    }


    /**
     * This need to get more OOP. Needs to throw exception
     * Parses values from String array into instance variables.
     */
    public void appendVariables() {
        if (inputArray.length > 8) {
            this.min_c_re = Double.parseDouble(this.inputArray[0]);
            this.min_c_im = Double.parseDouble(this.inputArray[1]);
            this.max_c_re = Double.parseDouble(this.inputArray[2]);
            this.max_c_im = Double.parseDouble(this.inputArray[3]);
            this.inf_n = Integer.parseInt(this.inputArray[4]);
            this.x = Integer.parseInt(this.inputArray[5]);
            this.y = Integer.parseInt(this.inputArray[6]);
            this.divider = Integer.parseInt(this.inputArray[7]);

            this.xStepSize = Math.floor(this.x / this.divider);
            this.yStepSize = Math.floor(this.y / this.divider);


        } else System.out.println("Wrong input");
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
    public void prepareWorkload() {
        double minX = this.min_c_re;
        double minY = this.min_c_im;
        double maxX = this.max_c_re;
        double maxY = this.max_c_im;
        String temp = "";

        double xInterval = maxX - minX;     // 2
        double yInterval = maxY - minY;     // 2

        double yDiff = yInterval / divider;
        double xDiff = xInterval / divider;


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

                temp = String.format("%f %f %f %f %d %f %f", minX, maxY, maxX, minY, inf_n, xStepSize, yStepSize);
                workPackages.add(temp);
            }
            // reset x-values
            minX = this.min_c_re;
            maxX = this.max_c_re;
        }
    }

    /**
     * Iterates over a list of sockets, creating a new
     * DataInputStream for each socket.
     * <p>
     * Amount of DataInputStreams will be the same as
     * parameter-list length
     */
    public ArrayList<DataInputStream> createInStreams(ArrayList<Socket> socketList) throws IOException {
        ArrayList<DataInputStream> inStreams = new ArrayList<>();
        for (int i = 0; i < socketList.size(); i++) {
            inStreams.add((new DataInputStream(socketList.get(i).getInputStream())));
        }
        return inStreams;
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
    public void receiveResult() throws IOException {

        // send status to all servers
        for (PrintWriter p : connectionOutList) {
            p.println("stage_ready_to_receive");
        }

        // receive result1 from server1, result2 from server2, ...
        int counter = 0;
        for (int i = 0; i < workPackages.size(); i++) {
            int length = dataInputList.get(counter).readInt();
            if (length > 0) {
                byte[] result = new byte[length];
                dataInputList.get(counter).readFully(result, 0, result.length); // read the result
                subArrayList.add(result);
            }
            counter++;
            if (counter == dataInputList.size()) counter = 0;
        }
        for (PrintWriter p : connectionOutList) {
            p.println("stage_results_received");
        }
    }


    /**
     * Sends out workload to user defined server(s)
     * Workload is sent as a prepared string which describes
     * boundaries.
     * <p>
     * Also send status to receiving end which is
     * Server class method receiveWork()
     */
    public void divideWorkBetweenServers() {

        // send intitial message to get through while loop on other end
        for (PrintWriter p : connectionOutList) {
            p.println(divider);
        }

        int amountOfServers = socketList.size();            //2
        int amountOfWorkPackages = workPackages.size();     //4

        int workPackCounter = 0;
        for (int i = 0; i < amountOfWorkPackages; i++) {
            connectionOutList.get(workPackCounter).println(workPackages.get(i));
            connectionOutList.get(workPackCounter).println("continue");
            workPackCounter++;
            if (workPackCounter == amountOfServers) workPackCounter = 0;
        }

        for (PrintWriter p : connectionOutList) {
            p.println("stage_WorkPackages_sent");
        }
    }

    /**
     * Creates PGM image from each workpack-result received.
     * The results are byte-arrays with signes bytes.
     * To convert signed byte value to integer, use bit operation: & 0xff.
     * ie (((byte)-127) & 0xff) == 255
     *
     * Image is named dynamically (nice) and stored in project folder for now.
     *
     */
    public void createSubResultImages() throws IOException {
        int counter = 0;
        for (byte[] b : subArrayList) {
            int maxvall = 255;
            String filename = "subResult" + (counter + 1) + ".pgm";
            String key = "subResult";
//            File file = File.createTempFile(key,".pgm",new File("Results"));
            File file = new File("Results" + File.separator + filename);


            PrintWriter pw = new PrintWriter(file);
            int width = (int) this.xStepSize;
            int height = (int) this.yStepSize;

            // magic number, width, height, and maxval
            pw.println("P2");
            pw.println(width + " " + height);
            pw.println(maxvall);

            // print out the data, limiting the line lengths to 70 characters
            int lineLength = 0;

//        int imagesize = this.subArrayList.size() * subArrayList.get(0).length;

            for (int i = 0; i < b.length; i++) {
                int value = subArrayList.get(counter)[i] & 0xff;

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
            counter++;
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
