package yuki135.mmdskin.texture.runtime;

import com.mojang.blaze3d.systems.RenderSystem;
import yuki135.mmdskin.bridge.runtime.NativeTexturePort;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/** 文件职责：封装纹理从 native 解码到 GPU 上传的无状态操作。 */
final class TextureGpuLoader {

    private TextureGpuLoader() {
    }

    static TextureRepository.Texture loadToGpu(String filename, NativeTexturePort port) {
        long handle = port.loadTexture(filename);
        if (handle == 0) return null;

        ByteBuffer buf = null;
        int texId = 0;
        int previousTexture = GL46C.glGetInteger(GL46C.GL_TEXTURE_BINDING_2D);
        try {
            int width = port.textureWidth(handle);
            int height = port.textureHeight(handle);
            long dataAddr = port.textureData(handle);
            boolean hasAlpha = port.textureHasAlpha(handle);
            int size = pixelBytes(width,height,hasAlpha);
            if(dataAddr==0)throw new IllegalArgumentException("Missing native texture pixels");
            buf = MemoryUtil.memAlloc(size);
            texId = GL46C.glGenTextures();
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, texId);
            port.copyTextureData(buf, dataAddr, size);
            buf.rewind();
            uploadPixels(width, height, hasAlpha, buf);
            configureTexture();
            return buildTexture(texId, width, height, hasAlpha);
        } catch (RuntimeException | Error e) {
            deleteGlTexture(texId);
            throw e;
        } finally {
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D,previousTexture);
            if(buf!=null)MemoryUtil.memFree(buf);
            port.deleteTexture(handle);
        }
    }

    static TextureRepository.PredecodedTexture decode(String filename, NativeTexturePort port) {
        long handle = port.loadTexture(filename);
        if (handle == 0) return null;
        ByteBuffer buf=null;
        try {
            int width = port.textureWidth(handle);
            int height = port.textureHeight(handle);
            long dataAddr = port.textureData(handle);
            boolean hasAlpha = port.textureHasAlpha(handle);
            int size = pixelBytes(width,height,hasAlpha);
            if(dataAddr==0)throw new IllegalArgumentException("Missing native texture pixels");
            buf = MemoryUtil.memAlloc(size);
            port.copyTextureData(buf, dataAddr, size);
            buf.rewind();
            var decoded=new TextureRepository.PredecodedTexture(buf, width, height, hasAlpha);
            buf=null;
            return decoded;
        } finally {
            if(buf!=null)MemoryUtil.memFree(buf);
            port.deleteTexture(handle);
        }
    }

    static TextureRepository.Texture uploadPredecoded(TextureRepository.PredecodedTexture pre) {
        int texId = GL46C.glGenTextures();
        int previousTexture = GL46C.glGetInteger(GL46C.GL_TEXTURE_BINDING_2D);
        try {
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, texId);
            uploadPixels(pre.width, pre.height, pre.hasAlpha, pre.pixelData());
            configureTexture();
            return buildTexture(texId, pre.width, pre.height, pre.hasAlpha);
        } catch (RuntimeException | Error e) {
            deleteGlTexture(texId);
            throw e;
        } finally {
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D,previousTexture);
            pre.release();
        }
    }

    static void deleteGlTexture(TextureRepository.Texture tex) {
        if (tex != null) {
            deleteGlTexture(tex.tex);
            tex.tex = 0;
        }
    }

    static void deleteGlTexture(int texId) {
        if (texId <= 0) return;
        if (RenderSystem.isOnRenderThreadOrInit()) {
            GL46C.glDeleteTextures(texId);
        } else {
            RenderSystem.recordRenderCall(() -> GL46C.glDeleteTextures(texId));
        }
    }

    private static void uploadPixels(int w, int h, boolean hasAlpha, ByteBuffer buf) {
        int size=pixelBytes(w,h,hasAlpha);
        if(buf==null||!buf.isDirect()||buf.remaining()<size)throw new IllegalArgumentException("Incomplete texture pixel buffer");
        if(w>GL46C.glGetInteger(GL46C.GL_MAX_TEXTURE_SIZE)||h>GL46C.glGetInteger(GL46C.GL_MAX_TEXTURE_SIZE))throw new IllegalArgumentException("Texture exceeds GPU limits");
        int[] parameters={GL46C.GL_UNPACK_ALIGNMENT,GL46C.GL_UNPACK_ROW_LENGTH,GL46C.GL_UNPACK_SKIP_ROWS,GL46C.GL_UNPACK_SKIP_PIXELS,GL46C.GL_UNPACK_IMAGE_HEIGHT,GL46C.GL_UNPACK_SKIP_IMAGES,GL46C.GL_UNPACK_SWAP_BYTES,GL46C.GL_UNPACK_LSB_FIRST};
        int[] saved=new int[parameters.length];
        for(int i=0;i<parameters.length;i++)saved[i]=GL46C.glGetInteger(parameters[i]);
        int unpackBuffer=GL46C.glGetInteger(GL46C.GL_PIXEL_UNPACK_BUFFER_BINDING);
        try {
            GL46C.glBindBuffer(GL46C.GL_PIXEL_UNPACK_BUFFER,0);
            for(int i=0;i<parameters.length;i++)GL46C.glPixelStorei(parameters[i],i==0?1:0);
            int format=hasAlpha?GL46C.GL_RGBA:GL46C.GL_RGB;
            GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D,0,format,w,h,0,format,GL46C.GL_UNSIGNED_BYTE,buf);
        } finally {
            for(int i=0;i<parameters.length;i++)GL46C.glPixelStorei(parameters[i],saved[i]);
            GL46C.glBindBuffer(GL46C.GL_PIXEL_UNPACK_BUFFER,unpackBuffer);
        }
    }

    static int pixelBytes(int width,int height,boolean alpha) {
        if(width<=0||height<=0)throw new IllegalArgumentException("Invalid texture dimensions");
        long pixels=(long)width*height;
        if(pixels>Integer.MAX_VALUE/(alpha?4:3))throw new IllegalArgumentException("Texture pixel size exceeds buffer limits");
        return (int)(pixels*(alpha?4:3));
    }

    private static void configureTexture() {
        GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAX_LEVEL, 0);
        GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_LINEAR);
        GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_LINEAR);
    }

    private static TextureRepository.Texture buildTexture(int texId, int w, int h, boolean hasAlpha) {
        TextureRepository.Texture t = new TextureRepository.Texture();
        t.tex = texId;
        t.hasAlpha = hasAlpha;
        t.vramSize = (long) w * h * (hasAlpha ? 4 : 3);
        return t;
    }
}
