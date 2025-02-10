package com.example.volumetric;

import java.util.ArrayList;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.opengl.Matrix;
import android.util.Log;

public class PlyRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "plyRenderer";
    private static final float SCALE = 0.179523f;
    private static final float TRANSLATE_X = -45.2095f;
    private static final float TRANSLATE_Y = 7.18301f;
    private static final float TRANSLATE_Z = -54.3561f;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int program;

    private static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 vPosition;\n" +
                    "attribute vec4 aColor; // 顶点颜色\n" +
                    "varying vec4 vColor;  // 传递给片段着色器的颜色\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * vPosition;\n" +
                    "    vColor = aColor; // 将颜色传递给片段着色器\n" +
                    "}";

    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;\n" +
                    "varying vec4 vColor; // 接收顶点颜色\n" +
                    "void main() {\n" +
                    "    gl_FragColor = vColor; // 使用顶点颜色\n" +
                    "}";

    private float[] vertices;
    private float[] colors;  // 用来存储顶点颜色

    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    // 用于控制帧的渲染
    private long lastFrameTime = 0;
    private boolean dataupdated = false;
    public PlyRenderer() {
    }

    public void updateData(ArrayList<Float> plyVertices) {
        // 将新的顶点和颜色数据更新到当前渲染数据
        int vertexIndex = 0;
        int colorIndex = 0;
        vertices = new float[plyVertices.size() / 6 * 3];
        colors = new float[plyVertices.size() / 6 * 3];

        for (int i = 0; i < plyVertices.size(); i++) {
            if (i % 6 < 3) {
                // 应用缩放和平移变换
                float rawCoord = plyVertices.get(i);
                switch (i % 3) {
                    case 0:
                        vertices[vertexIndex++] = rawCoord * SCALE + TRANSLATE_X;
                        break;
                    case 1:
                        vertices[vertexIndex++] = rawCoord * SCALE + TRANSLATE_Y;
                        break;
                    case 2:
                        vertices[vertexIndex++] = rawCoord * SCALE + TRANSLATE_Z;
                        break;
                }
            } else {
                colors[colorIndex++] = plyVertices.get(i);
            }
        }

        // 将数据放入顶点缓冲区和颜色缓冲区
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
        Log.d(TAG, "更新数据： " + vertices.length);
        dataupdated = true;
    }
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Set background color
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Enable depth testing

        // Compile shaders
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        //checkShaderCompile(vertexShader);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        // Create and link the program
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == GLES20.GL_FALSE) {
            String infoLog = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Program linking error: " + infoLog);
        }
        // Setup projection matrix
        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.perspectiveM(projectionMatrix, 0, 60.0f, 1.0f, 1.0f, 1000.0f);

        // Setup view matrix (camera)
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.translateM(viewMatrix, 0, 0.0f, -100.0f, -250.0f); // Move camera back
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(!dataupdated)
        {
            return;
        }
        //long currentTime = System.currentTimeMillis();
        // 每5秒接收并渲染一帧
//        if (currentTime - lastFrameTime >= 5000&&dataupdated) {
//            lastFrameTime = currentTime;
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                // 使用程序
                GLES20.glUseProgram(program);

                // 获取位置和颜色属性的句柄
                int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
                int colorHandle = GLES20.glGetAttribLocation(program, "aColor");
                int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

                // 设置MVP矩阵
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

                // 传递顶点坐标到着色器
                GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
                GLES20.glEnableVertexAttribArray(positionHandle);

                // 传递颜色数据到着色器
                GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 0, colorBuffer);
                GLES20.glEnableVertexAttribArray(colorHandle);

                // 绘制顶点
                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertices.length / 3);
            //dataupdated = false;
        //}
        dataupdated = false;
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // 计算屏幕的实际宽高比
        float aspectRatio = (float) width / height;

        // 使用实际宽高比设置投影矩阵
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, aspectRatio, 0.1f, 1000.0f);
    }

    // Helper function to compile shader
    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}