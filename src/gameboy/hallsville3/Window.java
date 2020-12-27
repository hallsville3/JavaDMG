package gameboy.hallsville3;

import javax.swing.*;

public class Window {
    JFrame frame;
    public Window(PPU ppu, int scale) {
        frame = new JFrame("GameBoy");
        frame.setSize(160 * scale, 144 * scale + 21);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        Screen screen = new Screen(ppu, scale);
        frame.add(screen);

        frame.setVisible(true);
    }
}
