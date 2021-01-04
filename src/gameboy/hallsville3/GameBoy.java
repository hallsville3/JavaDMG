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

    long time;

    public static int CLOCK_SPEED = 4194304;

    public GameBoy() {
        int memSize = 0xFFFF + 1;
        int scale = 4;
        controller = new Controller();
        apu = new APU();
        memory = new Memory(memSize, controller, apu);

        // Both controller and APU need access to the memory, but the memory needs them to. (Circular dependency)
        controller.setMemory(memory);
        apu.setMemory(memory);
        memory.memory[0xFF00] = 0xF;

        cpu = new CPU(memory);
        ppu = new PPU(memory, scale, this);
        timer = new Timer(memory);
        window = new Window(ppu, scale);

        window.frame.addKeyListener(controller);
    }

    public void loadGame(String game) throws IOException {
        cpu.loadGame(game);
    }

    public void run() {
        time = System.nanoTime();
        int clocks = 0;
        double fps = 60;
        while (cpu.pc < 0xFFFF) {
            // Emulate one instruction
            int interruptCycles = cpu.handleInterrupts();
            cpu.doCycle();

            int cycles = interruptCycles + cpu.cycles;
            timer.update(cycles);
            ppu.doCycle(cycles);
            clocks += cycles;
            apu.doCycle(cycles);

            if (clocks > CLOCK_SPEED / fps) {
                window.repaint(); // This call blocks until the screen is all done repainting
                vsync(fps);

                // Now that we have repainted we should add audio to be played
                apu.play();

                clocks -= CLOCK_SPEED / fps;
            }
        }
    }

    public void vsync(double fps) {
        // This method delays emulation to sync video to fps hz
        // It also reloads the audio buffer with enough audio
        long newTime = System.nanoTime();
        long dt = newTime - time; // Time since last sync

        long timeToSleep = Math.round(1000000000.0 / fps) - dt;

        long diff = 0;
        if (timeToSleep > 0) {
            try {
                long t1 = System.nanoTime();
                Thread.sleep(timeToSleep / 1000000, (int)(timeToSleep % 1000000));
                long t2 = System.nanoTime();
                diff = t2 - t1 - timeToSleep;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        time = System.nanoTime() - diff;
    }
}
