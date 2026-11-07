package yuki135.blacksouls.story;

import java.util.ArrayList;
import java.util.List;

public final class OriginalSkillAcquisitionState {
    public static final int SOURCE_WIDTH = 640;
    public static final int SOURCE_HEIGHT = 480;
    public static final int HOLD_FRAMES = 180;
    public static final int OPACITY_STEP = 32;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Boolean> occupied = new ArrayList<>();
    private long revision = -1;
    private long senRevision = -1;
    private long lastNanos;
    private long remainder;
    private boolean clockReady;

    public boolean accept(long incomingRevision, int skill, int actorOperand, boolean learned, boolean clear, long now) {
        if (incomingRevision < 1 || (clear ? skill != 0 || actorOperand != 0 || learned
                : skill != 340 || actorOperand != 2 && actorOperand != 14)) {
            throw new IllegalArgumentException("Invalid original skill acquisition state");
        }
        if (incomingRevision <= revision) return false;
        revision = incomingRevision;
        if (clear) {
            clearEntries();
            return true;
        }
        entries.add(new Entry(nextSlot(now), actorOperand, learned));
        return true;
    }

    public boolean acceptSen(long incomingRevision, int current, long delta, boolean clear, long now) {
        if (incomingRevision < 1 || (clear ? current != 0 || delta != 0 : delta == 0)) {
            throw new IllegalArgumentException("Invalid original SEN info state");
        }
        if (incomingRevision <= senRevision) return false;
        senRevision = incomingRevision;
        if (clear) {
            clearEntries();
            return true;
        }
        entries.add(new Entry(nextSlot(now), current, delta));
        return true;
    }

    private int nextSlot(long now) {
        if (entries.isEmpty()) {
            lastNanos = now;
            remainder = 0;
            clockReady = true;
        }
        int slot = occupied.indexOf(Boolean.FALSE);
        if (slot < 0) {
            slot = occupied.size();
            occupied.add(true);
        } else {
            occupied.set(slot, true);
        }
        return slot;
    }

    public void advanceFrame() {
        int size = entries.size();
        for (int index = 0; index < size; index++) {
            if (index >= entries.size()) continue;
            Entry entry = entries.get(index);
            entry.age++;
            if (entry.age < HOLD_FRAMES) {
                entry.opacity = Math.min(255, entry.opacity + OPACITY_STEP);
            } else {
                entry.y++;
                entry.opacity = Math.max(0, entry.opacity - OPACITY_STEP);
                if (entry.opacity == 0) {
                    occupied.set(entry.slot, false);
                    entries.remove(index);
                }
            }
        }
    }

    public void advance(long now, boolean paused) {
        if (!clockReady || entries.isEmpty()) {
            lastNanos = now;
            clockReady = true;
            return;
        }
        long elapsed = now - lastNanos;
        lastNanos = now;
        if (paused || elapsed <= 0) return;
        long seconds = elapsed / 1_000_000_000L;
        long fraction = elapsed % 1_000_000_000L * 60L + remainder;
        long frames = seconds > Long.MAX_VALUE / 60L ? Long.MAX_VALUE : seconds * 60L;
        frames = frames > Long.MAX_VALUE - fraction / 1_000_000_000L ? Long.MAX_VALUE : frames + fraction / 1_000_000_000L;
        remainder = fraction % 1_000_000_000L;
        while (frames-- > 0 && !entries.isEmpty()) advanceFrame();
    }

    public List<View> views() {
        return entries.stream().filter(entry -> entry.type == 3).map(Entry::view).toList();
    }

    public List<Notification> notifications() {
        return entries.stream().map(entry -> new Notification(entry.view(), entry.type, entry.current, entry.delta)).toList();
    }

    public long revision() {
        return revision;
    }

    public long senRevision() {
        return senRevision;
    }

    public void clearEntries() {
        entries.clear();
        occupied.clear();
        remainder = 0;
        clockReady = false;
    }

    public void reset() {
        clearEntries();
        revision = -1;
        senRevision = -1;
    }

    public static Canvas canvas(int width, int height) {
        if (width < 1 || height < 1) throw new IllegalArgumentException("Invalid original skill info canvas");
        double scale = Math.min(width / 640.0D, height / 480.0D);
        return new Canvas((width - 640.0D * scale) / 2.0D, (height - 480.0D * scale) / 2.0D, scale);
    }

    public record Canvas(double x, double y, double scale) {
    }

    public record View(int slot, int actorOperand, boolean learned, int age, int windowY, int opacity) {
        public int contentX() { return -4; }
        public int contentY() { return windowY + 12; }
        public int backgroundAlpha() { return opacity * 160 / 255; }
    }

    public record Notification(View window, int type, int current, long delta) {
    }

    private static final class Entry {
        private final int slot;
        private final int actorOperand;
        private final boolean learned;
        private final int type;
        private final int current;
        private final long delta;
        private int age;
        private int y;
        private int opacity;

        private Entry(int slot, int actorOperand, boolean learned) {
            this.slot = slot;
            this.actorOperand = actorOperand;
            this.learned = learned;
            type = 3;
            current = 0;
            delta = 0;
            y = 422 - Math.multiplyExact(slot, 40);
        }

        private Entry(int slot, int current, long delta) {
            this.slot = slot;
            actorOperand = 0;
            learned = false;
            type = 7;
            this.current = current;
            this.delta = delta;
            y = 422 - Math.multiplyExact(slot, 40);
        }

        private View view() {
            return new View(slot, actorOperand, learned, age, y, opacity);
        }
    }
}
