package gameboy.hallsville3;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;

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
    BufferedImage im;
    LCDControl control;
    Window window;
    int mode, modeCount; // Mode and cycles elapsed in that mode
    int internalWindowCounter;
    int scanlineCounter;
    int scale;

    public PPU (Memory memory, int sc) {
        this.memory = memory;
        im = new BufferedImage(160 * sc, 144 * sc, BufferedImage.TYPE_INT_RGB);
        control = new LCDControl(memory.read(0xFF40));
        mode = 2;
        modeCount = 0;
        scanlineCounter = 456; // CPU cycles per scanline
        internalWindowCounter = 0;
        scale = sc;
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

        Color[] greenPalette = {new Color(155, 188, 15), new Color(139, 172, 15),
                                new Color(48, 98, 48), new Color(15, 56, 15)};

        Color[] whitePalette = {Color.WHITE, Color.LIGHT_GRAY,
                                Color.DARK_GRAY, Color.BLACK};

        Color[][] palettes = {greenPalette, whitePalette};

        return palettes[1][color];
    }

    public void coincidenceCheck() {
        if ((memory.read(0xFF44) == memory.read(0xFF45))) { // Check LY = LYC
            memory.forceWrite(0xFF41, (char)(memory.read(0xFF41) | 1 << 2)); // Set coincidence bit
            if ((memory.read(0xFF41) & 1 << 6) == 1 << 6) {
                memory.setInterrupt(1);
            }
        } else {
            memory.forceWrite(0xFF41, (char)(memory.read(0xFF41) & ~(1 << 2))); // Reset coincidence
        }
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
                        coincidenceCheck();

                        if (memory.read(0xFF44) == 144) {
                            // Vblank interrupt
                            memory.setInterrupt(0);
                            // Switch to VBLANK
                            mode = 1;
                            if ((memory.read(0xFF41) & (1 << 4)) == 1 << 4) { // Do LCDC interrupt
                                memory.setInterrupt(1);
                            }
                            // Since we have a whole new screen we should repaint
                            window.frame.repaint();
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
                    internalWindowCounter = 0; // If we enter VBLANK the counter should be reset
                    if (modeCount >= 456) {
                        modeCount = 0;
                        memory.memory[0xFF44] = (char) (memory.read(0xFF44) + 1); // Increment LY

                        if (memory.read(0xFF44) == 154) { // Switch to OAM Read mode of line 0
                            memory.write(0xFF44, (char) 0);
                            mode = 2;
                            if ((memory.read(0xFF41) & (1 << 5)) == 1 << 5) { // Do LCDC interrupt
                                memory.setInterrupt(1);
                            }
                        }
                    }
                }
            }

            memory.forceWrite(0xFF41, (char)((memory.read(0xFF41) & ~0b11) | mode));

        } else { // If LCD is disabled, mode must be set to 1
            mode = 1;
            if ((memory.read(0xFF41) & (1 << 4)) == 1 << 4) { // Do LCDC interrupt
                memory.setInterrupt(1);
            }
            memory.forceWrite(0xFF41, (char)((memory.read(0xFF41) & ~0b111)));
            memory.write(0xFF44, (char)0);
        }
    }

    public int getPixel(int x, int y) {
        return im.getRGB(x, y);
    }

    public void setPixel(int x, int y, int rgb) {
        if (x >= 160 || y >= 144) {
            return; // Off screen
        }
        for (int i = 0; i < scale; i++) {
            for (int j = 0; j < scale; j++) {
                im.setRGB(x * scale + i, y * scale + j, rgb);
            }
        }
    }

    public void setPixel(int x, int y, Color c) {
        setPixel(x, y, c.getRGB());
    }

    public void draw(Graphics g) {
        g.drawImage(im, 0, 0, null);
    }

    public void clearScanline() {
        for (int x = 0; x < 160; x++) {
            setPixel(x, memory.read(0xFF44), Color.WHITE);
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

        char tileAddress = (char)((control.BGWindowTileDataSelect == 1) ? 0x8000 : 0x8800);
        char bgAddress;

        boolean windowWasDrawn = false;
        for (int x = 0; x < 160; x++) {
            boolean drawingWindow = false;
            if (control.WindowDisplayEnable == 1) {
                if (memory.read(0xFF44) >= windowY) { // This is the window
                    if (x >= windowX) {
                        drawingWindow = true;
                        windowWasDrawn = true; // The window is visible on this scanline
                    }
                }
            }

            if (drawingWindow) {
                bgAddress = (char) ((control.WindowTileMapDisplaySelect == 1) ? 0x9C00 : 0x9800);
            } else {
                bgAddress = (char) ((control.BGTileMapDisplaySelect == 1) ? 0x9C00 : 0x9800);
            }

            char yPos = (char)(drawingWindow ? internalWindowCounter: memory.read(0xFF44) + scrollY);
            char xPos = (char)(drawingWindow ? x - windowX : x + scrollX);

            char tileRow = (char)(yPos / 8);
            char tileCol = (char)(xPos / 8 % 32);
            char tile = memory.read(bgAddress + (tileCol + tileRow * 32) % 0x400);
            if (control.BGWindowTileDataSelect == 0) { // Signed addressing
                tile = (char)((tile + 128) & 0xFF);
            }
            char address = (char)(tileAddress + tile * 16);
            char byte1 = memory.read(address + (yPos % 8) * 2);
            char byte2 = memory.read(address + (yPos % 8) * 2 + 1);
            int j = (xPos % 8);
            char value = (char)((byte1 & (0b10000000 >> j)) >> (7-j));
            value |= (((byte2 & (0b10000000 >> j)) >> (7-j)) << 1);

            Color color = getPaletteColor(value, (char)0xFF47);
            setPixel(x, memory.read(0xFF44), color);
        }
        if (windowWasDrawn) {
            internalWindowCounter++;
        }
    }

    public void drawSprites() {
        class SpriteComparator implements Comparator<Integer> {
            public int compare(Integer a, Integer b) { // Sorts sprites by x coordinate
                return memory.read(0xFE00 + 4 * a + 1) - memory.read(0xFE00 + 4 * b + 1);
            }
        }

        ArrayList<Integer> sprites = new ArrayList<>(); // We will find the 10 leftmost visible sprites
        for (int sprite = 0; sprite < 40; sprite++) {
            char yPos = (char)(memory.read(0xFE00 + 4 * sprite) - 16);

            boolean use8x16 = control.OBJSpriteSize == 1;

            int ySize = (use8x16 ? 16 : 8);

            // This check determines if this line is in the sprite, and works with sprites that wrap around the top
            if ((char)(yPos + ySize) > memory.read(0xFF44) && (char)(yPos + ySize) - memory.read(0xFF44) <= ySize) {
                sprites.add(sprite);
            }
            if (sprites.size() == 10) {
                break;
            }
        }

        // Order the sprites by x coordinate
        sprites.sort(new SpriteComparator());

        ArrayList<Integer> drawnTo = new ArrayList<>(); // Only leftmost sprite gets to draw on a given square

        for (int sprite: sprites) {
            char yPos = (char)(memory.read(0xFE00 + 4 * sprite) - 16);
            char xPos = (char)(memory.read(0xFE00 + 4 * sprite + 1) - 8);

            char tile = memory.read(0xFE00 + 4 * sprite + 2);
            char atts = memory.read(0xFE00 + 4 * sprite + 3);
            boolean xFlip = (atts & 0b100000) == 0b100000;
            boolean yFlip = (atts & 0b1000000) == 0b1000000;
            boolean spritePriority = (atts & 0b10000000) == 0b10000000;

            boolean use8x16 = control.OBJSpriteSize == 1;

            char paletteAddress = (char)(((atts & 0b10000) == 0b10000) ? 0xFF49 : 0xFF48);

            int ySize = use8x16 ? 16 : 8;

            if (use8x16) {
                tile &= ~0b1; // Bit 0 should be ignored on 8x16 tiles
            }

            char address = (char)(0x8000 + tile * 16);

            char spriteLine = (char)(yFlip ? ySize - 1 - (memory.read(0xFF44) - yPos) : (memory.read(0xFF44) - yPos));
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
                char xPixel = (char)(xPos + j);
                if (xPixel + memory.read(0xFF44) * 160 < 144 * 160 && !drawnTo.contains(xPixel + memory.read(0xFF44) * 160)) {
                    if (spritePriority && getPixel(xPixel, memory.read(0xFF44)) != Color.WHITE.getRGB()) {
                        continue;
                    }
                    setPixel(xPixel, memory.read(0xFF44), color);
                    drawnTo.add(xPixel + memory.read(0xFF44) * 160);
                }
            }
        }
    }

    public void addWindow(Window w) {
        window = w;
    }
}
