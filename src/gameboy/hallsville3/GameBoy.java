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

    public static int CLOCK_SPEED = 4194304;

    public GameBoy() {
        int memSize = 0xFFFF + 1;
        int scale = 4;
        controller = new Controller();
        apu = new APU();
        memory = new Memory(memSize, controller, apu);
        controller.setMemory(memory);
        apu.setMemory(memory);
        memory.memory[0xFF00] = 0xF;

        cpu = new CPU(memory);
        ppu = new PPU(memory);
        timer = new Timer(memory);
        window = new Window(ppu, scale);

        window.frame.addKeyListener(controller);
    }

    public void loadGame(String game) throws IOException {
        cpu.loadGame(game);
    }

    public void run() {
        int count = 0;
        double fps = 60;
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
            if (count > CLOCK_SPEED / fps) {
                window.frame.repaint();
                count -= CLOCK_SPEED / fps;
            }
        }
    }
}
