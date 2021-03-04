package com.hzw.opengles.introduction;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.hzw.opengles.introduction.util.OpenGlUtils;

import java.nio.FloatBuffer;
import java.util.LinkedList;


/**
 * 负责显示一张图片
 */
public class GLImageHandler {

    // 数据中有多少个顶点，「管线就调用多少次顶点着色器」
//    public static final String NO_FILTER_VERTEX_SHADER = "" +
//            "attribute vec4 position;\n" + // 顶点着色器的顶点坐标,由外部程序传入
//            "attribute vec4 inputTextureCoordinate;\n" + // 传入的纹理坐标
//            "varying vec2 textureCoordinate;\n" +
//            " \n" +
//            "void main()\n" +
//            "{\n" +
//            "    gl_Position = position;\n" +
//            "    textureCoordinate = inputTextureCoordinate.xy;\n" + // 最终顶点位置
//            "}";

    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" + // 顶点着色器的顶点坐标,由外部程序传入
            "attribute vec4 inputTextureCoordinate;\n" + // 传入的纹理坐标
            "varying vec2 textureCoordinate;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = uMVPMatrix * position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" + // 最终顶点位置
            "}";

    // 光栅化后产生了多少个片段，就会插值计算出多少个 varying 变量，同时渲染管线就会调用多少次片段着色器
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" + // 最终顶点位置，上面顶点着色器的 varying 变量会传递到这里
            "uniform sampler2D inputImageTexture;\n" + // 外部传入的图片纹理 即代表整张图片的数据
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +  // 调用函数 进行纹理贴图
            "}";

    private final LinkedList<Runnable> mRunOnDraw;
    private final String mVertexShader;
    private final String mFragmentShader;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture;
    protected int mGLAttribTextureCoordinate;
    protected int mGLUniformPerspectiveMatrix;
    private final float[] mMVPMatrix = new float[16];

    public GLImageHandler() {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    public GLImageHandler(final String vertexShader, final String fragmentShader) {
        mRunOnDraw = new LinkedList<>();
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    public final void init() {
        // 编译链接着色器，创建着色器程序
        mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position"); // 顶点着色器的顶点坐标
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture"); // 传入的图片纹理
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId, "inputTextureCoordinate"); // 顶点着色器的纹理坐标
        mGLUniformPerspectiveMatrix = GLES20.glGetUniformLocation(mGLProgId, "uMVPMatrix");
    }

    public final void onChanged(int width, int height) {
        // https://blog.piasy.com/2016/06/07/Open-gl-es-android-2-part-1/index.html
        Matrix.perspectiveM(mMVPMatrix, 0, 45, (float) width / height, 0.1f, 100f);
        Matrix.translateM(mMVPMatrix, 0, -0f, 0f, -0.1f);
    }

    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        // 顶点着色器的顶点坐标
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        // 顶点着色器的纹理坐标
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        // 传入的图片纹理
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        GLES20.glUniformMatrix4fv(mGLUniformPerspectiveMatrix, 1, false, mMVPMatrix, 0);

        // 绘制顶点 ，方式有顶点法和索引法
        // GLES20.GL_TRIANGLE_STRIP即每相邻三个顶点组成一个三角形，为一系列相接三角形构成
        // 顶点法，按照传入渲染管线的顶点顺序及采用的绘制方式将顶点组成图元进行绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
