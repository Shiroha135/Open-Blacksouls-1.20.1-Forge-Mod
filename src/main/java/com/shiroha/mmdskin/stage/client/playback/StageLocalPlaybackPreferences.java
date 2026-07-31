package com.shiroha.mmdskin.stage.client.playback;

import com.shiroha.mmdskin.stage.domain.model.StageDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StageLocalPlaybackPreferences {
    private static final StageLocalPlaybackPreferences INSTANCE = new StageLocalPlaybackPreferences();

    private boolean customMotionEnabled;
    private String selectedPackName;
    private final LinkedHashSet<String> selectedMotionFiles = new LinkedHashSet<>();

    private StageLocalPlaybackPreferences() {
    }

    public static StageLocalPlaybackPreferences getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isCustomMotionEnabled() {
        return customMotionEnabled;
    }

    public synchronized void setCustomMotionEnabled(boolean enabled) {
        this.customMotionEnabled = enabled;
    }

    public synchronized List<String> getSelectedMotionFiles() {
        return new ArrayList<>(selectedMotionFiles);
    }

    public synchronized String getSelectedPackName() {
        return selectedPackName;
    }

    public synchronized void setSelectedPackName(String packName) {
        this.selectedPackName = isSafeName(packName) ? packName : null;
    }

    public synchronized boolean isSelected(String fileName) {
        return fileName != null && selectedMotionFiles.contains(fileName);
    }

    public synchronized void toggleMotionFile(String fileName) {
        if (!isSafeName(fileName)) {
            return;
        }
        if (!selectedMotionFiles.add(fileName)) {
            selectedMotionFiles.remove(fileName);
        }
    }

    public synchronized void retainAvailableMotionFiles(Collection<String> availableMotionFiles) {
        if (availableMotionFiles == null || availableMotionFiles.isEmpty()) {
            selectedMotionFiles.clear();
            return;
        }

        Set<String> available = new LinkedHashSet<>();
        for (String fileName : availableMotionFiles) {
            if (isSafeName(fileName)) {
                available.add(fileName);
            }
        }
        selectedMotionFiles.retainAll(available);
    }

    public synchronized StageDescriptor resolveDescriptor(StageDescriptor baseDescriptor, File stageDir) {
        if (baseDescriptor == null) {
            return null;
        }

        StageDescriptor resolved = baseDescriptor.copy();
        if (!customMotionEnabled || selectedMotionFiles.isEmpty() || stageDir == null || !stageDir.isDirectory()) {
            return resolved;
        }

        List<String> availableSelections = new ArrayList<>();
        for (String fileName : selectedMotionFiles) {
            if (!isSafeName(fileName)) {
                continue;
            }
            File file = new File(stageDir, fileName);
            if (file.isFile()) {
                availableSelections.add(fileName);
            }
        }

        if (!availableSelections.isEmpty()) {
            resolved.setMotionFiles(availableSelections);
        }
        return resolved;
    }

    public synchronized void reset() {
        customMotionEnabled = false;
        selectedPackName = null;
        selectedMotionFiles.clear();
    }

    private static boolean isSafeName(String fileName) {
        return fileName != null && !fileName.isEmpty()
                && !fileName.contains("..")
                && !fileName.contains("/")
                && !fileName.contains("\\");
    }
}
