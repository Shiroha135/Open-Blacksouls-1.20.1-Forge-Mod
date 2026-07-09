package com.BlackSouls.BlackSoulsMod.client.render;

import java.util.ArrayList;
import java.util.List;

public class VFXFrame {
    public List<VFXCell> cells = new ArrayList<>();

    public VFXFrame addCell(VFXCell cell) {
        this.cells.add(cell);
        return this;
    }
}