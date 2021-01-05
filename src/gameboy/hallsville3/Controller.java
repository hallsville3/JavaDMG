package gameboy.hallsville3;

import java.awt.event.*;

public class Controller implements KeyListener {
    char directions;
    char buttons;
    Memory memory;
    GameBoy gb;

    boolean reset;
    public Controller(GameBoy gb) {
        directions = 0xF;
        buttons = 0xF;
        this.gb = gb;
        reset = false;
    }

    public void setMemory(Memory mem) {
        memory = mem;
    }

    public void setInterrupt(int i) {
        memory.write(0xFF0F, (char) (memory.read(0xFF0F) | 1 << i));
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        boolean isButton = false;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_D -> directions &= ~(1);
            case KeyEvent.VK_A -> directions &= ~(1 << 1);
            case KeyEvent.VK_W -> directions &= ~(1 << 2);
            case KeyEvent.VK_S -> directions &= ~(1 << 3);


            case KeyEvent.VK_Q      -> {buttons &= ~(1); isButton = true;}
            case KeyEvent.VK_E      -> {buttons &= ~(1 << 1); isButton = true;}
            case KeyEvent.VK_SLASH  -> {buttons &= ~(1 << 2); isButton = true;}
            case KeyEvent.VK_ENTER  -> {buttons &= ~(1 << 3); isButton = true;}

            case KeyEvent.VK_R -> {reset = true;}
        }

        if (isButton && (memory.read(0xFF00) | 0b100000) == 0b100000) {
            setInterrupt(4);
        }

        if (!isButton && (memory.read(0xFF00) | 0b10000) == 0b10000) {
            setInterrupt(4);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_D -> directions |= (1);
            case KeyEvent.VK_A -> directions |= (1 << 1);
            case KeyEvent.VK_W -> directions |= (1 << 2);
            case KeyEvent.VK_S -> directions |= (1 << 3);


            case KeyEvent.VK_Q      -> buttons |= (1);
            case KeyEvent.VK_E      -> buttons |= (1 << 1);
            case KeyEvent.VK_SLASH  -> buttons |= (1 << 2);
            case KeyEvent.VK_ENTER  -> buttons |= (1 << 3);
        }
    }

    public char get(char value) {
        char bits = 0;
        if ((value & 0b10000) == 0) {
            bits = directions;
        }
        if ((value & 0b100000) == 0) {
            bits = buttons;
        }
        return (char)((value & 0xF0) | bits & 0xF);
    }

    public boolean doReset() {
        return reset;
    }
}
