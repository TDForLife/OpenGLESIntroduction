package com.hzw.opengles.introduction;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.hzw.opengles.introduction.util.OpenGlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * 加载纹理
 * 控制纹理位置
 * 控制纹理大小
 * 实现纹理动画
 */
public class GLShowImageActivity extends Activity {

    private static final String TAG = "zwt";

    // 绘制图片的原理：定义一组矩形区域的顶点，然后根据纹理坐标把图片作为纹理贴在该矩形区域内。
    // 原始的矩形区域的顶点坐标，因为后面使用了顶点法绘制顶点，所以不用定义绘制顶点的索引。无论窗口的大小为多少，在OpenGL二维坐标系中都是为下面表示的矩形区域
    static final float CUBE[] = { // 窗口中心为OpenGL二维坐标系的原点（0,0）
            -1.0f, -1.0f, // v1
            1.0f, -1.0f,  // v2
            -1.0f, 1.0f,  // v3
            1.0f, 1.0f,   // v4
    };

//    static final float CUBE[] = {
//            -0.5f, -0.5f, // v1
//            0.5f, -0.5f,  // v2
//            -0.5f, 0.5f,  // v3
//            0.5f, 0.5f,   // v4
//    };

    // 纹理也有坐标系，称 UV 坐标，或者 ST 坐标。
    // UV坐标定义为左上角（0，0），右下角（1，1），一张图片无论大小为多少，在UV坐标系中都是图片左上角为（0，0），右下角（1，1）
    // 纹理坐标，每个坐标的纹理采样对应上面顶点坐标。
    public static final float TEXTURE_NO_ROTATION[] = {
            0.0f, 1.0f, // v1
            1.0f, 1.0f, // v2
            0.0f, 0.0f, // v3
            1.0f, 0.0f, // v4
    };

//    public static final float TEXTURE_NO_ROTATION[] = {
//            0.0f, 0.5f, // v1
//            0.5f, 0.5f, // v2
//            0.0f, 0.0f, // v3
//            0.5f, 0.0f, // v4
//    };

    private GLSurfaceView mGLSurfaceView;
    private int mGLTextureId = OpenGlUtils.NO_TEXTURE; // 纹理id
    private final GLImageHandler mGLImageHandler = new GLImageHandler();

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;
    private int mOutputWidth, mOutputHeight; // 窗口大小
    private int mImageWidth, mImageHeight; // Bitmap 图片实际大小
    private long mLastDrawFrameTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_01);
        mGLSurfaceView = findViewById(R.id.gl_surfaceview);

        // 创建OpenGL ES 2.0 的上下文环境
        mGLSurfaceView.setEGLContextClientVersion(2);
        // 设置 GLSurfaceView 背景支持透明模式
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mGLSurfaceView.setZOrderOnTop(true);
        // 设置 Render
        mGLSurfaceView.setRenderer(new MyRender());
        // 设置刷新方式 - 手动刷新
        // mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private class MyRender implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // 刷新 Surface 的背景
            GLES20.glClearColor(1, 0, 0, 0.3f);
            // 当我们需要绘制透明图片时，就需要关闭它
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);

            mGLImageHandler.init();

            // 需要显示的图片
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.thelittleprince);
            mImageWidth = bitmap.getWidth();
            mImageHeight = bitmap.getHeight();

            // 把图片数据加载进 GPU，生成对应的纹理对象 ID
            mGLTextureId = OpenGlUtils.loadTexture(bitmap, mGLTextureId, true); // 加载纹理

            // 顶点数组缓冲器
            mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mGLCubeBuffer.put(CUBE).position(0);

            // 纹理数组缓冲器
            mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d(TAG, "onSurfaceChanged width - " + width);
            Log.d(TAG, "onSurfaceChanged height - " + height);
            mOutputWidth = width;
            mOutputHeight = height;
            // 设置窗口大小
            // GLES20.glViewport(0, 0, 800, 800);
            GLES20.glViewport(0, 0, width, height);
            // 调整图片显示大小。如果不调用该方法，则会导致图片整个拉伸到填充窗口显示区域
            adjustImageScaling();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            // 根据纹理id，顶点和纹理坐标数据绘制图片
            mGLImageHandler.onDraw(mGLTextureId, mGLCubeBuffer, mGLTextureBuffer);
        }

        // 调整图片显示大小为居中显示
        private void adjustImageScaling() {
            float outputWidth = mOutputWidth;
            float outputHeight = mOutputHeight;

            float ratio1 = outputWidth / mImageWidth;
            float ratio2 = outputHeight / mImageHeight;
            float ratioMax = Math.min(ratio1, ratio2);

            // 居中后图片显示的大小
            int imageWidthNew = Math.round(mImageWidth * ratioMax);
            int imageHeightNew = Math.round(mImageHeight * ratioMax);

            // 图片被拉伸的比例
            float ratioWidth = outputWidth / imageWidthNew;
            float ratioHeight = outputHeight / imageHeightNew;

            // 根据拉伸比例还原顶点
            float[] cube = new float[]{
                        CUBE[0] / ratioWidth, CUBE[1] / ratioHeight,
                        CUBE[2] / ratioWidth, CUBE[3] / ratioHeight,
                        CUBE[4] / ratioWidth, CUBE[5] / ratioHeight,
                        CUBE[6] / ratioWidth, CUBE[7] / ratioHeight,
                };

            mGLCubeBuffer.clear();
            mGLCubeBuffer.put(cube).position(0);
        }
    }

    /**
     * 统计帧的刷新间隔，在 onDrawFrame 中调用
     */
    private void statisticsFrameRefreshInternal() {
        long currentTime = System.currentTimeMillis();
        Log.d(TAG, "onDrawFrame.." + (currentTime - mLastDrawFrameTime));
        mLastDrawFrameTime = currentTime;
    }
}
