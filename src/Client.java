import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;


public class Client {
    private double min_c_re;
    private double min_c_im;
    private double max_c_re;
    private double max_c_im;
    private int x;
    private int y;
    private int inf_n;
    private int devider;
    private Coordinate[][] twoDInputArray;
    private double xStepSize;
    private double yStepSize;
    private ArrayList<String> serverList;
    private ArrayList<Socket> socketList = new ArrayList<>();
    private ArrayList<byte[]> subArrayList = new ArrayList<>();
    private ArrayList<String> workPackages = new ArrayList<>();
    private ArrayList<PrintWriter> connectionOutList = new ArrayList<>();
    private ArrayList<DataInputStream> dataInputList = new ArrayList<>();
    private String[] inputArray;
    private String exampleInput = "-1 -1 1 1 1024 800 800 2 localhost:8001 localhost:8002";

    Client() {
        Scanner sc = new Scanner(System.in);
        inputArray = Parser.parse(exampleInput);
        serverList = Parser.separateServers(inputArray);
        this.appendVariables();
        System.out.println(subArrayList.size());
        this.prepareWorkload();

        try {
//          String input = sc.nextLine();
            socketList = this.createSockets(serverList);
            connectionOutList = this.createOutStreams(socketList);
            dataInputList = createInStreams(socketList);
            devideWorkBetweenServers2();
            System.out.println("utanfor reveic funktion");
            receiveAnything();
            System.out.println("subArrayList: " + subArrayList.size());
            System.out.println("workpackList: " + workPackages.size());
            createSubResultImages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
    }

    public ArrayList<Socket> createSockets(ArrayList<String> serverList) throws IOException {
        ArrayList<Socket> sockets = new ArrayList<>();
        for (String s : serverList) {
            String[] temp = s.split(":");
            sockets.add(new Socket(temp[0], Integer.parseInt(temp[1])));
        }
        return sockets;
    }

    public ArrayList<PrintWriter> createOutStreams(ArrayList<Socket> socketList) throws IOException {
        ArrayList<PrintWriter> outStreams = new ArrayList<>();
        for (int i = 0; i < socketList.size(); i++) {
            outStreams.add((new PrintWriter(socketList.get(i).getOutputStream(), true)));
        }
        return outStreams;
    }

    public void appendVariables() {
        if (inputArray.length > 8) {
            this.min_c_re = Double.parseDouble(this.inputArray[0]);
            this.min_c_im = Double.parseDouble(this.inputArray[1]);
            this.max_c_re = Double.parseDouble(this.inputArray[2]);
            this.max_c_im = Double.parseDouble(this.inputArray[3]);
            this.inf_n = Integer.parseInt(this.inputArray[4]);
            this.x = Integer.parseInt(this.inputArray[5]);
            this.y = Integer.parseInt(this.inputArray[6]);
            this.devider = Integer.parseInt(this.inputArray[7]);

            this.xStepSize = Math.floor(this.x / this.devider);
            this.yStepSize = Math.floor(this.y / this.devider);


        } else System.out.println("Wrong input");
    }

    public void prepareWorkload() {
        double minX = this.min_c_re;
        double minY = this.min_c_im;
        double maxX = this.max_c_re;
        double maxY = this.max_c_im;
        String temp = "";

        double xInterval = maxX - minX;
        double yInterval = maxY - minY;

        minY = maxY; // for correct decrement for minY after first iteration
        for (int i = 0; i < devider; i++) {
            maxY = maxY - ((yInterval / devider) * i);
            minY = maxY - ((yInterval / devider));
            for (int j = 0; j < devider; j++) {
                maxX = minX + ((xInterval / devider) * (j + 1));
                minX = minX + ((xInterval / devider) * j);

                temp = String.format("%f %f %f %f %d %f %f", minX, maxY, maxX, minY, inf_n, xStepSize, yStepSize);
                workPackages.add(temp);
//              out.println(temp);
            }
            minX = this.min_c_re;
            maxX = this.max_c_re;
        }
    }

    public ArrayList<DataInputStream> createInStreams(ArrayList<Socket> socketList) throws IOException {
        ArrayList<DataInputStream> inStreams = new ArrayList<>();
        for (int i = 0; i < socketList.size(); i++) {
            inStreams.add((new DataInputStream(socketList.get(i).getInputStream())));
        }
        return inStreams;
    }

    public void receiveAnything() throws IOException {

        for (PrintWriter p : connectionOutList) {
            p.println("stage_ready_to_receive");
        }
        System.out.println("inuti receive funktionen!");
        int counter = 0;

        for (int i = 0; i < workPackages.size(); i++) {
            int length = dataInputList.get(counter).readInt();
            if (length > 0) {
                byte[] message = new byte[length];
                dataInputList.get(counter).readFully(message, 0, message.length); // read the message
                subArrayList.add(message);
            }
            counter++;
            if (counter == dataInputList.size()) counter = 0;
        }
        for (PrintWriter p : connectionOutList) {
            p.println("stage_results_received");
        }
    }

    public void devideWorkBetweenServers2() {
        // todo vill antagligen skicka workpackage i ordning.

        int amountOfServers = socketList.size();        //2
        int amountOfWorkPackages = workPackages.size(); //4

        int workPackCounter = 0;
        for (int i = 0; i < amountOfWorkPackages; i++) {
            connectionOutList.get(workPackCounter).println(workPackages.get(i));
            workPackCounter++;
            if (workPackCounter == amountOfServers) workPackCounter = 0;

        }
        for (PrintWriter p : connectionOutList) {
            p.println("stage_WorkPackages_sent");
        }
    }


    /**
     * Creates a PGM file from the given image.
     *
     * @param filename name of the file to be created
     * @throws FileNotFoundException
     */
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
        for (int i = 0; i < devider; i++) {

            for (int j = 0; j < devider; j++) {
                tempList.add(subArrayList.get(j + indexAdjuster));
            }
            subAreaRows.add(tempList);
            indexAdjuster = +devider;
            tempList = new ArrayList<>();
        }

        for (ArrayList<byte[]> list : subAreaRows) {        // 1 och 2
            for (int i = 1; i <= yStepSize; i++) {
                for (int j = 0; j < devider; j++) {
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
