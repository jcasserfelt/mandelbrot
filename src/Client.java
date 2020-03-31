import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;


public class Client {
    private double min_c_re;
    private double min_c_im;
    private double max_c_re;
    private double max_c_im;
    private int x;
    private int y;
    private int inf_n;
    private int divider;
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
    private String exampleInput = "-0.76 0.04 -0.75 0.05 1024 4800 4800 4 localhost:8001 localhost:8002";


    public static void main(String[] args) {
        if (args.length < 9) {
            System.out.println("Not enought input parameters");
            return;
        }
        try {
            String input = createInputString(args);
            ArrayList<String> serverList99 = separateServers(args);
            try {
                Client client = new Client(input, serverList99);
            } catch (
                    IOException e) {
                e.printStackTrace();
            }
        } catch (NumberFormatException e) {
            System.out.println("Wrong input format");
        }


    }

    public static String createInputString(String[] input) throws NumberFormatException {
        String result;

        double min_c_re = Double.parseDouble(input[0]);
        double min_c_im = Double.parseDouble(input[1]);
        double max_c_re = Double.parseDouble(input[2]);
        double max_c_im = Double.parseDouble(input[3]);
        int inf_n = Integer.parseInt(input[4]);
        int x = Integer.parseInt(input[5]);
        int y = Integer.parseInt(input[6]);
        int divider = Integer.parseInt(input[7]);

        result = String.format("%f %f %f %f %d %d %d %d", min_c_re, min_c_im, max_c_re, max_c_im, inf_n, x, y, divider);
        // "-0.76 0.04 -0.75 0.05 1024 4800 4800 4 localhost:8001 localhost:8002";
        // min_c_re min_c_im max_c_re max_c_im max_n x y divisions list-of-servers
        return result;
    }


    Client(String values, ArrayList<String> servers) throws IOException {
        exampleInput = values;
        serverList = servers;

        this.inputArray = Parser.parse(exampleInput);
//        serverList = Parser.separateServers(inputArray);

        Scanner sc = new Scanner(System.in);
        this.appendVariables();
//        System.out.println(subArrayList.size());
        this.prepareWorkload();                // new n fresh

        try {
//          String input = sc.nextLine();
            socketList = createSockets(serverList);
            connectionOutList = createOutStreams(socketList);
            dataInputList = createInStreams(socketList);
            divideWorkBetweenServers();
            System.out.println("utanfor reveic funktion");
            receiveAnything();
            System.out.println("subArrayList: " + subArrayList.size());
            System.out.println("workpackList: " + workPackages.size());
            createSubResultImages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> separateServers(String[] inputArray) {
        ArrayList<String> serverList = new ArrayList<>();
        for (int i = 8; i < inputArray.length; i++) {
            serverList.add(inputArray[i]);
        }

        return serverList;
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
        if (inputArray.length > 7) {
            this.min_c_re = Double.valueOf(this.inputArray[0].replace(",", "."));
            this.max_c_re = Double.valueOf(this.inputArray[2].replace(",", "."));
            this.max_c_im = Double.valueOf(this.inputArray[3].replace(",", "."));
            this.inf_n = Integer.parseInt(this.inputArray[4].replace(",", "."));
            this.x = Integer.parseInt(this.inputArray[5]);
            this.y = Integer.parseInt(this.inputArray[6]);
            this.divider = Integer.parseInt(this.inputArray[7]);

            this.xStepSize = Math.floor(this.x / this.divider);
            this.yStepSize = Math.floor(this.y / this.divider);


        } else System.out.println("Wrong input");
    }


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
        System.out.println("check that sweet workpak");

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

    public void divideWorkBetweenServers() {

        // send intitial message to get through while loop on other end
        for (PrintWriter p : connectionOutList) {
            p.println(divider);
        }

        int amountOfServers = socketList.size();        //2
        int amountOfWorkPackages = workPackages.size(); //4

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
