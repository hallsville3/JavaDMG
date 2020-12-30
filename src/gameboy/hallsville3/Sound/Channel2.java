package gameboy.hallsville3.Sound;

import gameboy.hallsville3.GameBoy;
import gameboy.hallsville3.Memory;

public class Channel2 implements SoundChannel {
    public int timer, envelopeTimer;
    public int envelopeMode;
    public int sweep;

    public boolean envelopeFinished;

    public int dutyIndex;
    public int duty;
    public int[] dutyCycles;

    public int volume;

    public int soundLength;
    public Memory memory;

    public boolean enabled;

    public Channel2(Memory mem) {
        memory = mem;
        timer = 0;
        dutyIndex = 0;
        duty = 0;
        soundLength = 0;
        dutyCycles = new int[] {0b00000001, 0b10000001, 0b10000111, 0b01111110};
        enabled = false;

        volume = 10;
    }

    public void updateTimer(int cpuCycles) {
        timer -= cpuCycles;
        if (timer <= 0) {
            dutyIndex = (dutyIndex + 1) % 8;
            int x = memory.read(0xFF18) | ((memory.read(0xFF19) & 0b111) << 8);
            timer = 4 * (2048 - x);
            soundLength = memory.read(0xFF16) & 0b00111111;
        }
    }

    public void updateEnvelope(int cpuCycles) {
        if (envelopeFinished) {
            return;
        }
        envelopeTimer += cpuCycles;
        if (envelopeTimer >= GameBoy.CLOCK_SPEED / 64 * sweep) {
            envelopeTimer = 0;
            volume += envelopeMode;
            if (volume == 16) {
                envelopeFinished = true;
                volume = 15;
            } else if (volume == -1) {
                envelopeFinished = true;
                volume = 0;
            }
        }
    }

    public void trigger() {
        envelopeTimer = 0;
        char envelopeRegister = memory.read(0xFF17);
        volume = envelopeRegister >> 4;
        envelopeMode = (envelopeRegister & 0b00001000) == 0b00001000 ? 1 : -1;
        sweep = envelopeRegister & 0b111;
        envelopeFinished = false;
        envelopeTimer = 0;
    }

    public byte doCycle(int cpuCycles) {
        updateTimer(cpuCycles);

        updateEnvelope(cpuCycles);

        duty = dutyCycles[memory.read(0xFF16) >> 6];
        boolean toggle = (duty & (1 << dutyIndex)) == 1 << dutyIndex;
        enabled = (memory.read(0xFF17) & 0b11100000) != 0;

        if (!enabled) {
            return 0;
        }
        return (byte) (toggle ? volume * 4 : 0);
    }
}
