package junk;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Modulo extends Canvas {

    public static void main(String[] args) {
        new Modulo();
    }

    final private int ITERATIONS = 255;

    public static byte calculatePoint(float x, float y) {
        int ITERATIONS = 256;

        float cx = x;
        float cy = y;

        int i = 0;
        for (i = 0; i < ITERATIONS; i++) {
            float nx = x * x - y * y + cx;
            float ny = 2 * x * y + cy;
            x = nx;
            y = ny;

            if (x * x + y * y > 2) {
                System.out.println("unstable: " + i);
                System.out.println("unstable: " + (byte) i + " (castad)");
                return (byte) i;

            }
        }
        if (i == ITERATIONS) {
            System.out.println("stable: "+ i);
            System.out.println("stable: "+ (byte) i + "(castad)");
            return (byte) 255;
        }

        return (byte) 255;
    }


    public Modulo() {
        int w = 900;
        int h = 900;
        int SCALE = 400;


        byte testByte = -126;
        byte[] byteArray = new byte[w * h];
        int[] intArray = new int[w * h];

        int k = 0;
        byte temp;
        for (int i = 0; i < w; i++) {
            System.out.println("/////////////////////////////// NY RAAAAD ///////////////////////////////");
            for (int j = 0; j < h; j++) {
                temp = Modulo.calculatePoint((j - w / 2f) / SCALE, (i - h / 2f) / SCALE);
                byteArray[k] = temp;
                System.out.println("byteArray " + byteArray[k] );
                intArray[k] = (byte) (byteArray[k] & 0xFF);
                if (k % 100 == 0) System.out.println("---------------- HUNDRA PIXLAR BREDD ----------------");
//                System.out.println(byteArray[k] + " treated");
                System.out.println("----------");
//                byteArray[k] = -1;
                k++;
            }
        }

//
//        int counter = 0;
//        for (int i = 0; i < w * h; i++) {
//            byteArray[i] = (byte) (counter % 256);
//            counter++;
//            System.out.println(byteArray[i]);
//        }


        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED);

        image.getRaster().setDataElements(0, 0, w, h, byteArray);


        SwingUtilities.invokeLater(new

                                           Runnable() {
                                               @Override
                                               public void run() {
                                                   JFrame frame = new JFrame(getClass().getSimpleName());
                                                   frame.add(new JLabel(new ImageIcon(image)));
                                                   frame.setSize(400, 400);
                                                   frame.setResizable(true);
                                                   frame.pack();
                                                   frame.setLocationRelativeTo(null);
                                                   frame.setVisible(true);

                                               }
                                           });
    }


}
