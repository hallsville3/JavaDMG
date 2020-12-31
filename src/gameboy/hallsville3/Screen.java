package gameboy.hallsville3;

import javax.swing.*;
import java.awt.*;

public class Screen extends JPanel {
    PPU ppu;
    public Screen(PPU ppu) {
        this.ppu = ppu;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(0, 0,0));
        ppu.draw(g);
    }

}
