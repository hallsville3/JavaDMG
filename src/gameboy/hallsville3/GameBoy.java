package gameboy.hallsville3;

import gameboy.hallsville3.Sound.APU;

import java.io.IOException;

public class GameBoy {
    Memory memory;
    CPU cpu;
    PPU ppu;
    APU apu;
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
        apu = new APU(memory);

        window = new Window(ppu, scale);
        window.frame.addKeyListener(controller);
        window.frame.repaint();
    }

    public void loadGame(String game) throws IOException {
        cpu.loadGame(game);
    }

    public void run() {
        int count = 0;
        double fps = 60;
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
            apu.doCycle(cycles);
            if (count > 4194304 / fps) {
                window.frame.repaint();
                count -= 4194304 / fps;
            }
        }
    }
}
