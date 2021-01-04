package gameboy.hallsville3;

import javax.swing.*;
import java.awt.*;

public class Window {
    public int repaints;
    JFrame frame;
    Screen screen;
    PPU ppu;
    public Window(PPU ppu, int scale) {
        frame = new JFrame("GameBoy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        screen = new Screen(ppu);
        screen.setPreferredSize(new Dimension(160 * scale, 144 * scale));
        frame.add(screen);
        frame.pack();

        frame.setVisible(true);

        ppu.addWindow(this);
        this.ppu = ppu;
        repaints = 0;
    }

    public void repaint() {
        repaints++;
        frame.repaint();
    }
}
