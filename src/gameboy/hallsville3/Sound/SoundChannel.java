package gameboy.hallsville3.Sound;

public abstract class SoundChannel {
    char nr0, nr1, nr2, nr3, nr4;
    public SoundChannel(char n) {
        nr0 = n;
        nr1 = (char)(n+1);
        nr2 = (char)(n+2);
        nr3 = (char)(n+3);
        nr4 = (char)(n+4);
    }
    abstract byte doCycle(int cpuCycles);
    abstract void trigger();
    abstract void load();
    abstract int getID();
}
