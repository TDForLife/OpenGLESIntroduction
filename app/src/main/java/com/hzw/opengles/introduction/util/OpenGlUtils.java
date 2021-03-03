/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hzw.opengles.introduction.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.IntBuffer;

public class OpenGlUtils {
    public static final int NO_TEXTURE = -1;

    public static void deleteTexture(int textureID) {
        GLES20.glDeleteTextures(1, new int[]{
                textureID
        }, 0);
    }

    public static int loadTexture(final Bitmap img, final int usedTexId) {
        return loadTexture(img, usedTexId, true);
    }

    /**
     * GLES20.glActiveTexture 一般在 glBindTexture 之前执行，单纹理的创建不需要激活
     * https://www.jianshu.com/p/5e0d05b1d003
     * https://blog.xujiaji.com/post/Learn-OpenGL-Lesson-Six
     * @param img
     * @param usedTexId
     * @param recycle
     * @return
     */
    public static int loadTexture(final Bitmap img, final int usedTexId, final boolean recycle) {
        int[] textures = new int[]{-1};
        if (usedTexId == NO_TEXTURE) {
            // GPU 创建一张纹理（1 表示想要生成的纹理的数量），即「纹理对象」，Texture Object
            GLES20.glGenTextures(1, textures, 0);
            // GLES20.GL_TEXTURE_2D 描述的是一个「纹理目标」，即 Texture Target
            // 有个 Texture Target 前置概念，叫 Texture Unit，即「纹理单元」，一个纹理单元可以存着多个纹理目标
            // glBindTexture 将纹理对象，绑定到当前被激活的纹理单元上的纹理目标，这个绑定行为，其实就决定了这个纹理对象的类型
            // 一旦绑定后，这个纹理的类型就确定了，无法将它绑定到其他纹理目标
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            // 设置纹理属性 - 放大过滤器 - 线性放大
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            // 设置纹理属性 - 缩小过滤器 - 线性缩小
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            // 纹理也有坐标系，称 UV 坐标，或者 ST 坐标
            // S 轴的拉伸方式为重复，决定采样值的坐标超出图片范围时的采样方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            // T 轴的拉伸方式为重复
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            // 将 Bitmap 传递到已经绑定的纹理中
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            // 纹理已经存在了，就执行纹理替换
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if (recycle) {
            img.recycle();
        }
        // 返回纹理对象
        return textures[0];
    }

    public static int loadTexture(final IntBuffer data, final Size size, final int usedTexId) {
        int[] textures = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, size.width, size.height,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, size.width,
                    size.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
            textures[0] = usedTexId;
        }
        return textures[0];
    }

    public static int loadTextureAsBitmap(final IntBuffer data, final Size size, final int usedTexId) {
        Bitmap bitmap = Bitmap
                .createBitmap(data.array(), size.width, size.height, Config.ARGB_8888);
        return loadTexture(bitmap, usedTexId);
    }

    /**
     * 加载着色器
     *
     * @param strSource
     * @param iType
     * @return
     */
    public static int loadShader(final String strSource, final int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.d("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }

    /**
     * 加载着色器程序
     *
     * @param strVSource
     * @param strFSource
     * @return
     */
    public static int loadProgram(final String strVSource, final String strFSource) {
        int iVShader;
        int iFShader;
        int iProgId;

        iVShader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER); // 顶点着色器
        if (iVShader == 0) {
            Log.d("Load Program", "Vertex Shader Failed");
            return 0;
        }
        iFShader = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER); // 片元着色器
        if (iFShader == 0) {
            Log.d("Load Program", "Fragment Shader Failed");
            return 0;
        }

        iProgId = GLES20.glCreateProgram();

        GLES20.glAttachShader(iProgId, iVShader);
        GLES20.glAttachShader(iProgId, iFShader);

        GLES20.glLinkProgram(iProgId);
        // 获取program的链接情况
        int[] link = new int[1];
        GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0);
        if (link[0] <= 0) {
            Log.d("Load Program", "Linking Failed");
            return 0;
        }
        GLES20.glDeleteShader(iVShader);
        GLES20.glDeleteShader(iFShader);
        return iProgId;
    }

    public static float rnd(final float min, final float max) {
        float fRandNum = (float) Math.random();
        return min + (max - min) * fRandNum;
    }
}
