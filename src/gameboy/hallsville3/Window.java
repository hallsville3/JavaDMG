package gameboy.hallsville3;

import javax.swing.*;
import java.awt.*;

public class Window {
    JFrame frame;
    static int barHeight = 28;
    public Window(PPU ppu, int scale) {
        frame = new JFrame("GameBoy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        Screen screen = new Screen(ppu, scale);
        screen.setPreferredSize(new Dimension(160 * scale, 144 * scale));
        frame.add(screen);
        frame.pack();

        frame.setVisible(true);
    }
}
