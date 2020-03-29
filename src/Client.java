import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
    byte[] resultArray;
    Coordinate[][] twoDInputArray;
    double xStepSize;
    double yStepSize;

    String userInput;
    String parameterInputs;
    ArrayList<String> serverList = new ArrayList<>();
    ArrayList<Socket> socketList = new ArrayList<>();
    ArrayList<byte[]> subArrayList = new ArrayList<>();
//    ArrayList<byte[]> subResultList = new ArrayList<>();

    ArrayList<String> workPackages = new ArrayList<>();
    ArrayList<PrintWriter> connectionOutList = new ArrayList<>();
    ArrayList<DataInputStream> dataInputList = new ArrayList<>();

    ArrayList<ArrayList<String>> workPackagesLists = new ArrayList<>();


    //    private BufferedReader in;
    DataInputStream in;
    private PrintWriter out;
    private PrintWriter out2;
    String[] inputArray;
    String[] serverArray;
    String exampleInput = "-1 -1 1 1 1024 800 800 2 localhost:8001 localhost:8002";

    Client() {
        Scanner sc = new Scanner(System.in);
        inputArray = Parser.parse(exampleInput);
        serverList = Parser.separateServers(inputArray);
        this.appendVariables();
        resultArray = new byte[x * y];
//        this.createSubArrays();
        System.out.println(subArrayList.size());
        this.prepareWorkload();

        try {
//          String input = sc.nextLine();
            socketList = this.createSockets(serverList);
            connectionOutList = this.createOutStreams(socketList);

//
            devideWorkBetweenServers2();
            dataInputList = createInStreams(socketList);
            receiveAnything();
            System.out.println("subArrayList: " + subArrayList.size());
            System.out.println("workpackList: " + workPackages.size());
//            out = new PrintWriter(socket.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


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

    // not needed
    public void createSubArrays() {
        double amountsOfArrays = this.devider * this.devider;  // ie 4
        double totalElements = this.x * this.y;                  // ie 10000
        for (int i = 0; i < amountsOfArrays; i++) {
            this.subArrayList.add(new byte[(int) (totalElements / amountsOfArrays)]);
        }
    }

    // trash
    public void prepareDivisionBoundaries() {
        String temp = "";
        for (int i = 1; i <= devider * devider; i++) {
            double min_c_re = this.min_c_re / (this.devider * this.devider) * i;
            double min_c_im = this.min_c_im / (this.devider * this.devider) * i;
            double max_c_re = this.max_c_re / (this.devider * this.devider) * i;
            double max_c_im = this.max_c_im / (this.devider * this.devider) * i;
            temp = String.format("%f %f %f %f %d %f %f", min_c_re, min_c_im, max_c_re, max_c_im, inf_n, xStepSize, yStepSize);
            workPackages.add(temp);
//            out.println(min_c_re);
//            out.println(temp);
//            out.println(inf_n);
        }
//        out.close();
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


    public void devideWorkBetweenServers() {
        // todo vill antagligen skicka workpackage i ordning.

        int amountOfServers = socketList.size();        //2
        int amountOfWorkPackages = workPackages.size(); //4
        double workPackagesPerServer = workPackages.size() / amountOfServers;


        for (int i = 0; i < amountOfWorkPackages; i++) {
            if (i < workPackagesPerServer) {
                connectionOutList.get(0).println(workPackages.get(i));
            }
            if (i >= workPackagesPerServer) {
                connectionOutList.get(1).println(workPackages.get(i));
            }
        }
        // todo server tar emot workpack, och behandlar innan tar emot nästa


        for (PrintWriter p : connectionOutList) {
            p.close();
        }
    }

    // obsolete
    public void populate2dArray() {
        double xInterval = max_c_re - min_c_re;
        double yInterval = max_c_im - min_c_im;

        double tempX = min_c_re;
        double tempY = max_c_im;

        double xAdd;
        double ySub;

        twoDInputArray = new Coordinate[x][y];
        for (int i = 0; i < x; i++) {
            if (i != 0) {
                xAdd = (xInterval / (x - 1));
                tempX = tempX + xAdd;
            }
            tempY = max_c_im;
            for (int j = 0; j < y; j++) {
                twoDInputArray[i][j] = new Coordinate(tempX, tempY);
                ySub = (yInterval / (y - 1));
                tempY = tempY - ySub;
            }
        }
    }

    public ArrayList<DataInputStream> createInStreams(ArrayList<Socket> socketList) throws IOException {
        ArrayList<DataInputStream> inStreams = new ArrayList<>();
        for (int i = 0; i < socketList.size(); i++) {
            inStreams.add((new DataInputStream(socketList.get(i).getInputStream())));
        }
        return inStreams;
    }

    // obsolete
    public void receivSubResults() throws IOException {
        int amountOfServers = socketList.size();        //2
        int amountOfWorkPackages = workPackages.size(); //4
        double workPackagesPerServer = workPackages.size() / amountOfServers;
// l'gg i subArrayList
        int workPackCounter = 0;
        for (int i = 0; i < amountOfWorkPackages; i++) {
            int length = dataInputList.get(workPackCounter).readInt();                    // read length of incoming message
            length = 160000;
            if (length > 0) {
                byte[] message = new byte[length];
                dataInputList.get(workPackCounter).readFully(message, 0, message.length); // read the message

            }

//            subArrayList.get(workPackCounter) = dataInputList.get(workPackCounter).readAllBytes();

//            while ((dataInputList.get(workPackCounter).read(subArrayList.get(workPackCounter))) != 0) {
//                workPackCounter++;
//
//                dataInputList.get(workPackCounter).read(subArrayList.get(workPackCounter));
//
//                if (workPackCounter == workPackagesPerServer) workPackCounter = 0;
//            }
//            subArrayList.get(workPackCounter
        }
    }

    public void receiveAnything() throws IOException {
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
    }

    public void devideWorkBetweenServers2() {
        // todo vill antagligen skicka workpackage i ordning.

        int amountOfServers = socketList.size();        //2
        int amountOfWorkPackages = workPackages.size(); //4
        double workPackagesPerServer = workPackages.size() / amountOfServers;

        int workPackCounter = 0;
        for (int i = 0; i < amountOfWorkPackages; i++) {
            connectionOutList.get(workPackCounter).println(workPackages.get(i));
            workPackCounter++;
            if (workPackCounter == amountOfServers) workPackCounter = 0;

        }
//            if (i < workPackagesPerServer) {
//                connectionOutList.get(0).println(workPackages.get(i));
//            }
//            if (i >= workPackagesPerServer) {
//                connectionOutList.get(1).println(workPackages.get(i));
//            }

        // todo server tar emot workpack, och behandlar innan tar emot nästa


        for (PrintWriter p : connectionOutList) {
//            p.close();
        }
    }
}