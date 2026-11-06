package yuki135.mmdskin.config;

public interface RuntimeConfigPort {
    RuntimeConfigPort DEFAULT = new RuntimeConfigPort() {
        public boolean isPhysicsEnabled() { return true; }
        public boolean isGpuSkinningEnabled() { return false; }
        public boolean isMmdShaderEnabled() { return false; }
    };
    static RuntimeConfigPort defaults() { return DEFAULT; }
    static RuntimeConfigPort fromConfigManager() {
        return new RuntimeConfigPort() {
            public boolean isPhysicsEnabled() { return ConfigManager.isPhysicsEnabled(); }
            public boolean isGpuSkinningEnabled() { return ConfigManager.isGpuSkinningEnabled(); }
            public boolean isMmdShaderEnabled() { return ConfigManager.isMMDShaderEnabled(); }
        };
    }
    boolean isPhysicsEnabled();
    boolean isGpuSkinningEnabled();
    boolean isMmdShaderEnabled();
}
