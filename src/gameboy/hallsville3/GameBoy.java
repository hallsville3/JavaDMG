package gameboy.hallsville3;

import java.io.IOException;

public class GameBoy {
    Memory memory;
    CPU cpu;
    PPU ppu;
    Controller controller;
    Window window;
    public GameBoy() {
        int memSize = 0xFFFF+1;
        int scale = 4;
        controller = new Controller();
        memory = new Memory(memSize, controller);
        controller.setMemory(memory);
        memory.memory[0xFF00] = 0xF;
        cpu = new CPU(memory);
        ppu = new PPU(memory);

        window = new Window(ppu, scale);
        window.frame.addKeyListener(controller);
        window.frame.repaint();
    }

    public void loadGame(String game) throws IOException {
        cpu.loadGame(game);
    }

    //TODO Implement Timer

    public void run() throws InterruptedException {
        int count = 0;
        long time = System.currentTimeMillis();
        while (cpu.pc < 0xFFFF) {
            // Emulate one cycle
            cpu.handleInterrupts();
            cpu.doCycle();
            count += cpu.cycles;

            ppu.doCycle(cpu.cycles);
            if (count > 4194304 / 60) {
                window.frame.repaint();
                long newtime = System.currentTimeMillis();
                if (17 - (newtime - time) > 0) {
                    Thread.sleep(17 - (newtime - time));
                }
                time = newtime;
                count = 0;
            }
        }
    }
}
