package gameboy.hallsville3.Sound;

public interface SoundChannel {
    byte doCycle(int cpuCycles);
    void trigger();
}
