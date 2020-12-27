package gameboy.hallsville3;

import java.awt.*;

class LCDControl {
    public char LCDDisplayEnable, WindowTileMapDisplaySelect, WindowDisplayEnable, BGWindowTileDataSelect;
    public char BGTileMapDisplaySelect, OBJSpriteSize, OBJSpriteDisplayEnable, BGDisplay;
    public LCDControl(char control) {
        update(control);
    }

    public void update(char control) {
        LCDDisplayEnable =              (char)((control & (1 << 7)) >> 7);
        WindowTileMapDisplaySelect =    (char)((control & (1 << 6)) >> 6);
        WindowDisplayEnable =           (char)((control & (1 << 5)) >> 5);
        BGWindowTileDataSelect =        (char)((control & (1 << 4)) >> 4);
        BGTileMapDisplaySelect =        (char)((control & (1 << 3)) >> 3);
        OBJSpriteSize =                 (char)((control & (1 << 2)) >> 2);
        OBJSpriteDisplayEnable =        (char)((control & (1 << 1)) >> 1);
        BGDisplay =                     (char)(control & 1);
    }
}

public class PPU {
    Memory memory;
    Color[] screenBuffer; // Where the pixel data is stored
    LCDControl control;
    int mode, modeCount; // Mode and cycles elapsed in that mode
    int scanlineCounter;

    public PPU (Memory memory) {
        this.memory = memory;
        screenBuffer = new Color[160 * 144];
        for (int i = 0; i < 160 * 144; i++) {
            screenBuffer[i] = Color.WHITE;
        }
        control = new LCDControl(memory.read(0xFF40));
        mode = 2;
        modeCount = 0;
        scanlineCounter = 456; // CPU cycles per scanline
    }

    public Color getPaletteColor(char colorID, char address) {
        char palette = memory.read(address);
        char colorBitShift = 0;
        switch (colorID) { // Case 0 gives 0
            case 1 -> colorBitShift = 2;
            case 2 -> colorBitShift = 4;
            case 3 -> colorBitShift = 6;
        }

        char color = (char)((palette >> colorBitShift) & 0b11);
        // Now color contains the actual color from the palette

        Color result = Color.WHITE;

        switch (color) { // Case 0 gives White
            case 1 -> result = new Color(161, 161, 161);
            case 2 -> result = Color.DARK_GRAY;
            case 3 -> result = Color.BLACK;
        }

        return result;
    }

    public void doCycle(int cpuCycles) {
        control.update(memory.read(0xFF40));

        modeCount += cpuCycles;
        if (control.LCDDisplayEnable == 1) {
            switch (mode) {
                case 2 -> // OAM Read Mode (80 Cycles)
                {
                    if (modeCount >= 80) {
                        modeCount = 0;
                        mode = 3;
                    }
                }
                case 3 -> // VRAM read mode (172 Cycles)
                {
                    if (modeCount >= 172) {
                        modeCount = 0;
                        drawScanline();
                        mode = 0;
                        if ((memory.read(0xFF41) & (1 << 3)) == 1 << 3) { // Do LCDC interrupt
                            memory.setInterrupt(1);
                        }
                    }
                }
                case 0 -> // HBLANK (204 Cycles)
                {
                    if (modeCount >= 204) {
                        modeCount = 0;
                        memory.memory[0xFF44] = (char) (memory.read(0xFF44) + 1); // Increment LY

                        if (memory.read(0xFF44) == 144) {
                            // Vblank interrupt
                            memory.setInterrupt(0);
                            // Switch to VBLANK
                            mode = 1;
                            if ((memory.read(0xFF41) & (1 << 4)) == 1 << 4) { // Do LCDC interrupt
                                memory.setInterrupt(1);
                            }
                        } else {
                            // Switch to OAM Read of next line
                            mode = 2;
                            if ((memory.read(0xFF41) & (1 << 5)) == 1 << 5) { // Do LCDC interrupt
                                memory.setInterrupt(1);
                            }
                        }
                    }

                }
                case 1 -> // VBLANK (456 * 10 Cycles)
                {
                    if (modeCount >= 456) {
                        modeCount = 0;
                        memory.memory[0xFF44] = (char) (memory.read(0xFF44) + 1); // Increment LY

                        if (memory.read(0xFF44) > 153) { // Switch to OAM Read mode of line 0
                            memory.write(0xFF44, (char) 0);
                            mode = 2;
                            if ((memory.read(0xFF41) & (1 << 5)) == 1 << 5) { // Do LCDC interrupt
                                memory.setInterrupt(1);
                            }
                        }
                    }
                }
            }

            if ((memory.read(0xFF44) == memory.read(0xFF45))) {
                memory.forceWrite(0xFF41, (char)(memory.read(0xFF41) | 1 << 2)); // Set coincidence bit
                if ((memory.read(0xFF41) & 1 << 6) == 1 << 6) {
                    memory.setInterrupt(1);
                }
            } else {
                memory.forceWrite(0xFF41, (char)(memory.read(0xFF41) & ~(1 << 2))); // Reset coincidence
            }

            memory.forceWrite(0xFF41, (char)((memory.read(0xFF41) & ~0b11) | mode));

        } else { // If LCD is disabled, mode must be set to 1
            mode = 1;
            memory.forceWrite(0xFF41, (char)((memory.read(0xFF41) & ~0b11) | mode));
            memory.write(0xFF44, (char)0);
        }
    }

    public void draw(Graphics g, int scale) {
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 160; x++) {
                g.setColor(screenBuffer[y * 160 + x]);
                g.fillRect(x * scale, y * scale, scale, scale);
            }
        }
    }

    public void clearScanline() {
        for (int x = 0; x < 160; x++) {
            screenBuffer[x + memory.read(0xFF44) * 160] = Color.WHITE;
        }
    }

    public void drawScanline() {
        if (control.BGDisplay == 1) {
            drawTiles();
        } else {
            clearScanline();
        }

        if (control.OBJSpriteDisplayEnable == 1) {
            drawSprites();
        }
    }

    public void drawTiles() {
        int scrollY = memory.read(0xFF42) & 0xFF;
        int scrollX = memory.read(0xFF43) & 0xFF;

        int windowY = memory.read(0xFF4A);
        int windowX = memory.read(0xFF4B) - 7;

        boolean drawingWindow = false;
        if (control.WindowDisplayEnable == 1) {
            if (windowY <= memory.read(0xFF44)) { // This is the window
                drawingWindow = true;
            }
        }

        char tileAddress = (char)((control.BGWindowTileDataSelect == 1) ? 0x8000 : 0x8800);
        char bgAddress;

        if (drawingWindow) {
            bgAddress = (char) ((control.WindowTileMapDisplaySelect == 1) ? 0x9C00 : 0x9800);
        } else {
            bgAddress = (char) ((control.BGTileMapDisplaySelect == 1) ? 0x9C00 : 0x9800);
        }

        for (int x = 0; x < 160; x++) {
            char yPos = (char)(drawingWindow ? memory.read(0xFF44) - windowY: memory.read(0xFF44) + scrollY);
            char xPos = (char)(drawingWindow && x >= windowX ? x - windowX : x + scrollX);
            char tileRow = (char)(yPos / 8 * 32);
            char tileCol = (char)(xPos / 8);
            char tile = memory.read(bgAddress + (tileCol + tileRow) % (32 * 32));
            if (control.BGWindowTileDataSelect == 0) {
                tile = (char)((tile + 128) & 0xFF);
            }
            char address = (char)(tileAddress + tile * 16);
            char byte1 = memory.read(address + (memory.read(0xFF44) % 8) * 2);
            char byte2 = memory.read(address + (memory.read(0xFF44) % 8) * 2 + 1);
            int j = x % 8;
            char value = (char)((byte1 & (0b10000000 >> j)) >> (7-j));
            value |= (((byte2 & (0b10000000 >> j)) >> (7-j)) << 1);

            Color color = getPaletteColor(value, (char)0xFF47);
            screenBuffer[x + memory.read(0xFF44) * 160] = color;
        }
    }

    public void drawSprites() {
        int spriteCount = 0; // Only 10 allowed per line
        for (int sprite = 0; sprite < 40; sprite++) {
            char yPos = (char)(memory.read(0xFE00 + 4 * sprite) - 16);
            char xPos = (char)(memory.read(0xFE00 + 4 * sprite + 1) - 8);
            char tile = memory.read(0xFE00 + 4 * sprite + 2);
            char atts = memory.read(0xFE00 + 4 * sprite + 3);
            boolean xFlip = (atts & 0b100000) == 0b100000;
            boolean yFlip = (atts & 0b1000000) == 0b1000000;
            boolean spritePriority = (atts & 0b10000000) == 0b10000000;

            boolean use8x16 = control.OBJSpriteSize == 1;

            char paletteAddress = (char)(((atts & 0b10000) == 0b10000) ? 0xFF49 : 0xFF48);

            if (yPos > memory.read(0xFF44) || memory.read(0xFF44) >= yPos + (use8x16 ? 16 : 8) || spriteCount == 10) {
                continue;
            }
            spriteCount++;

            char address = (char)(0x8000 + tile * 16);

            char spriteLine = (char)(yFlip ? 7 - (memory.read(0xFF44) - yPos) : (memory.read(0xFF44) - yPos));
            char byte1 = memory.read(address + spriteLine * 2);
            char byte2 = memory.read(address + spriteLine * 2 + 1);
            for (int j = 0; j < 8; j++) {
                int xValue = xFlip ? 7 - j : j;
                char value = (char)((byte1 & (0b10000000 >> xValue)) >> (7-xValue));
                value |= (((byte2 & (0b10000000 >> xValue)) >> (7-xValue)) << 1);
                if (value == 0) { // Color 0 is transparent
                    continue;
                }
                Color color = getPaletteColor(value, paletteAddress);
                if ((xPos + j) + memory.read(0xFF44) * 160 < 144 * 160) {
                    screenBuffer[(xPos + j) + memory.read(0xFF44) * 160] = color;
                }
            }
        }
    }
}
