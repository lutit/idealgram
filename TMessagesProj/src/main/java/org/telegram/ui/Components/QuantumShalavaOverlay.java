package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.view.MotionEvent;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class QuantumShalavaOverlay extends GLSurfaceView {

    private final QuantumRenderer renderer;
    private boolean started;

    public QuantumShalavaOverlay(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        setPreserveEGLContextOnPause(true);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        renderer = new QuantumRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setVisibility(GONE);
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        setVisibility(VISIBLE);
        renderer.resetTime();
        onResume();
    }

    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        setVisibility(GONE);
        onPause();
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private static class QuantumRenderer implements Renderer {
        private static final float[] QUAD = new float[] {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        };

        private final FloatBuffer vertexBuffer;
        private int program;
        private int positionHandle;
        private int timeHandle;
        private int resolutionHandle;

        private int width;
        private int height;
        private volatile long startTimeMs;

        QuantumRenderer() {
            ByteBuffer bb = ByteBuffer.allocateDirect(QUAD.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(QUAD);
            vertexBuffer.position(0);
        }

        void resetTime() {
            startTimeMs = SystemClock.uptimeMillis();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            program = buildProgram(
                    AndroidUtilities.readRes(R.raw.quantum_shalava_vert),
                    AndroidUtilities.readRes(R.raw.quantum_shalava_frag)
            );
            if (program == 0) {
                return;
            }
            positionHandle = GLES20.glGetAttribLocation(program, "p");
            timeHandle = GLES20.glGetUniformLocation(program, "t");
            resolutionHandle = GLES20.glGetUniformLocation(program, "r");
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glClearColor(0f, 0f, 0f, 0f);
            resetTime();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            this.width = width;
            this.height = height;
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (program == 0) {
                return;
            }
            if (width <= 0 || height <= 0) {
                return;
            }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(program);

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            float time = (SystemClock.uptimeMillis() - startTimeMs) / 1000.0f;
            GLES20.glUniform1f(timeHandle, time);
            GLES20.glUniform2f(resolutionHandle, (float) width, (float) height);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }

        private static int buildProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (vertexShader == 0 || fragmentShader == 0) {
                return 0;
            }
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                FileLog.e("QuantumShalavaOverlay, link error: " + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                return 0;
            }
            return program;
        }

        private static int loadShader(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            if (shader == 0) {
                return 0;
            }
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                FileLog.e("QuantumShalavaOverlay, compile error: " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                return 0;
            }
            return shader;
        }
    }
}
