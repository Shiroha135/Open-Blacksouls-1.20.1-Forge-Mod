package com.shiroha.mmdskin.render.material;

import java.util.ArrayDeque;
import java.util.Deque;

public final class EntityRenderOpacity {
    private static final ThreadLocal<Deque<Scope>> SCOPES = new ThreadLocal<>();

    private EntityRenderOpacity() {
    }

    public static float current() {
        Deque<Scope> scopes = SCOPES.get();
        return scopes == null || scopes.isEmpty() ? 1.0F : scopes.peek().opacity;
    }

    public static boolean isScoped() {
        Deque<Scope> scopes = SCOPES.get();
        return scopes != null && !scopes.isEmpty();
    }

    public static float clamp(float opacity) {
        return Float.isFinite(opacity) ? Math.max(0.0F, Math.min(1.0F, opacity)) : 0.0F;
    }

    public static float apply(float materialAlpha) {
        return clamp(materialAlpha) * current();
    }

    public static Scope open(float opacity) {
        Deque<Scope> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        Scope scope = new Scope(Thread.currentThread(), current() * clamp(opacity));
        scopes.push(scope);
        return scope;
    }

    public static final class Scope implements AutoCloseable {
        private final Thread owner;
        private final float opacity;
        private boolean closed;

        private Scope(Thread owner, float opacity) {
            this.owner = owner;
            this.opacity = opacity;
        }

        @Override
        public void close() {
            if (closed) return;
            Deque<Scope> scopes = SCOPES.get();
            if (Thread.currentThread() != owner || scopes == null || scopes.peek() != this) {
                throw new IllegalStateException("Entity opacity scopes must close in owner-thread LIFO order");
            }
            scopes.pop();
            closed = true;
            if (scopes.isEmpty()) SCOPES.remove();
        }
    }
}
