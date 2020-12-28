package gameboy.hallsville3;

import java.io.IOException;

public class GameBoy {
    Memory memory;
    CPU cpu;
    PPU ppu;
    Timer timer;
    Controller controller;
    Window window;
    public GameBoy() {
        int memSize = 0xFFFF+1;
        int scale = 7;
        controller = new Controller();
        memory = new Memory(memSize, controller);
        controller.setMemory(memory);
        memory.memory[0xFF00] = 0xF;
        cpu = new CPU(memory);
        ppu = new PPU(memory);
        timer = new Timer(memory);

        window = new Window(ppu, scale);
        window.frame.addKeyListener(controller);
        window.frame.repaint();
    }

    public void loadGame(String game) throws IOException {
        cpu.loadGame(game);
    }

    public void run() throws InterruptedException {
        int count = 0;
        long time = System.currentTimeMillis();
        while (cpu.pc < 0xFFFF) {
            // Emulate one cycle
            int cycles = 0;
            cycles += cpu.handleInterrupts();
            cpu.doCycle();
            cycles += cpu.cycles;

            count += cycles;

            timer.update(cycles);
            ppu.doCycle(cycles);
            if (count > 4194304 / 60) {
                window.frame.repaint();
                long newTime = System.currentTimeMillis();
                if (17 - (newTime - time) > 0) {
                    //noinspection BusyWait
                    Thread.sleep(17 - (newTime - time));
                }
                time = newTime;
                count = 0;
            }
        }
    }
}
