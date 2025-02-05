package com.example.volumetric;

import android.os.Bundle;
import android.util.Log; // 导入 Log 类
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // 定义 TAG 变量
    private static final String TAG = "MainActivity";
    private String serverIp = "192.168.1.1";  // 替换为服务端IP
    private int serverPort = 12345;  // 替换为服务端端口
    private GLSurfaceView glSurfaceView;
    private PlyRenderer plyRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // 使用OpenGL ES 2.0
        // 启动线程从服务器接收数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(serverIp, serverPort);
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                    // 读取数据长度（例如先读取一个int来表示数据大小）
                    int dataLength = dataInputStream.readInt();
                    byte[] compressedData = new byte[dataLength];

                    // 接收压缩后的数据
                    dataInputStream.readFully(compressedData);

                    // 解压数据
                    DracoDecoder decoder = new DracoDecoder();
                    byte[] decodedData = decoder.decodeDraco(compressedData);
                    ArrayList<Float> plyVertices = loadPlyData(decodedData);

                    // 更新UI，准备渲染数据
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            plyRenderer = new PlyRenderer(plyVertices);
                            glSurfaceView.setRenderer(plyRenderer);
                        }
                    });

                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Failed to connect to server", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
        plyRenderer = new PlyRenderer(loadPlyData("longdress_vox10_1051.ply"));
        glSurfaceView.setRenderer(plyRenderer);

        setContentView(glSurfaceView);
    }

    // 测试函数：读取PLY文件中的顶点数据
    private ArrayList<Float> loadPlyData(String filePath) {
        ArrayList<Float> vertices = new ArrayList<>();
        try {
            InputStream inputStream = getAssets().open(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    if (line.contains("end_header")) {
                        isHeader = false; // 头部结束，开始读取数据
                    }
                } else {
                    String[] tokens = line.trim().split(" ");
                    if (tokens.length >= 3) {
                        // 只读取前3个数，作为顶点坐标
                        vertices.add(Float.parseFloat(tokens[0])); // x
                        vertices.add(Float.parseFloat(tokens[1])); // y
                        vertices.add(Float.parseFloat(tokens[2])); // z

                        // 读取颜色值，并规范化为 [0, 1] 范围
                        vertices.add(Float.parseFloat(tokens[3]) / 255.0f); // red
                        vertices.add(Float.parseFloat(tokens[4]) / 255.0f); // green
                        vertices.add(Float.parseFloat(tokens[5]) / 255.0f); // blue
                    }
                }
            }
            reader.close();

            // 打印最终的 vertices 值
//            Log.d(TAG, "Vertices size: " + vertices.size());
//            for (int i = 0; i < vertices.size(); i += 3) {
//                float x = vertices.get(i);
//                float y = vertices.get(i + 1);
//                float z = vertices.get(i + 2);
//                Log.d(TAG, "Vertex [" + (i / 3) + "]: x=" + x + ", y=" + y + ", z=" + z);
//            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load PLY file", Toast.LENGTH_LONG).show();
        }
        return vertices;
    }
    private ArrayList<Float> loadPlyData(byte[] byteArray) {
        ArrayList<Float> vertices = new ArrayList<>();
        try {
            // 使用 ByteArrayInputStream 从字节数组中读取数据
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            BufferedReader reader = new BufferedReader(new InputStreamReader(byteArrayInputStream));
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    if (line.contains("end_header")) {
                        isHeader = false; // 头部结束，开始读取数据
                    }
                } else {
                    String[] tokens = line.trim().split(" ");
                    if (tokens.length >= 3) {
                        // 只读取前3个数，作为顶点坐标
                        vertices.add(Float.parseFloat(tokens[0])); // x
                        vertices.add(Float.parseFloat(tokens[1])); // y
                        vertices.add(Float.parseFloat(tokens[2])); // z

                        // 读取颜色值，并规范化为 [0, 1] 范围
                        vertices.add(Float.parseFloat(tokens[3]) / 255.0f); // red
                        vertices.add(Float.parseFloat(tokens[4]) / 255.0f); // green
                        vertices.add(Float.parseFloat(tokens[5]) / 255.0f); // blue
                    }
                }
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load PLY file", Toast.LENGTH_LONG).show();
        }
        return vertices;
    }
}