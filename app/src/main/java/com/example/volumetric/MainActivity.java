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
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // 定义 TAG 变量
    private static final String TAG = "MainActivity";
    private String serverIp = "192.168.182.128";  // 替换为服务端IP
    private int serverPort = 12348;  // 替换为服务端端口
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
                    Log.d(TAG, "尝试连接到服务器: " + serverIp + ":" + serverPort);
                    Socket socket = new Socket(serverIp, serverPort);
                    Log.d(TAG, "成功连接到服务器");
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                    // 读取数据长度（例如先读取一个int来表示数据大小）
                    int dataLength = dataInputStream.readInt();
                    dataLength = Integer.reverseBytes(dataLength);
                    Log.d(TAG, "读取到的数据长度: " + dataLength);
                    byte[] compressedData = new byte[dataLength];
                    int bytesRead = 0;
                    // 接收压缩后的
                    try {
                        while (bytesRead < dataLength) {
                            // 计算剩余数据长度
                            int remaining = dataLength - bytesRead;
                            // 每次读取最大 1024 字节（可以根据情况调整缓冲区大小）
                            int chunkSize = Math.min(remaining, 1024);
                            int read = dataInputStream.read(compressedData, bytesRead, chunkSize);

                            if (read == -1) {
                                throw new IOException("服务器连接关闭，无法接收数据");
                            }

                            bytesRead += read;
                            //Log.d(TAG, "已接收 " + bytesRead + " 字节，剩余 " + (dataLength - bytesRead) + " 字节");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "数据接收失败: " + e.getMessage());
                    }


                    Log.d(TAG, "成功接收压缩后的数据");

                    // 解压数据

                    Log.d(TAG, "开始解压数据");
                    DracoDecoder decoder = new DracoDecoder();
                    byte[] decodedData = decoder.decodeDraco(compressedData);
//                    // 打印部分解压后的 PLY 数据内容（例如打印前1000个字节）
//                    int bytesToPrint = Math.min(1000, decodedData.length);
//                    String decodedDataString = new String(decodedData, 0, bytesToPrint);  // 转换为字符串
//                    Log.d(TAG, "Decoded Data (first " + bytesToPrint + " bytes): " + decodedDataString);
//                    Log.d(TAG, "decodedData size: " + decodedData.length);
                    ArrayList<Float> plyVertices = loadPlyData(decodedData);
                    // 打印最终的 vertices 值
            Log.d(TAG, "Vertices size: " + plyVertices.size());
            for (int i = 0; i < plyVertices.size(); i += 3) {
                float x = plyVertices.get(i);
                float y = plyVertices.get(i + 1);
                float z = plyVertices.get(i + 2);
                Log.d(TAG, "Vertex [" + (i / 3) + "]: x=" + x + ", y=" + y + ", z=" + z);
            }
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
//        plyRenderer = new PlyRenderer(loadPlyData("longdress_vox10_1051.ply"));
//        glSurfaceView.setRenderer(plyRenderer);
//
//        setContentView(glSurfaceView);
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
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

            String line;
            boolean isHeader = true;
            int vertexCount = 0;  // 用来存储顶点的数量

            // 读取 PLY 文件的头部
            while (isHeader) {
                line = readLine(dataInputStream);
                if (line == null) break;
                if (line.contains("element vertex")) {
                    // 获取顶点数
                    String[] tokens = line.split(" ");
                    vertexCount = Integer.parseInt(tokens[2]);
                }
                if (line.contains("end_header")) {
                    isHeader = false;  // 头部结束，开始读取数据
                }
            }

            Log.d(TAG, "vertexCount " + vertexCount);
            // 读取顶点数据部分
            for (int i = 0; i < vertexCount; i++) {
                // 读取每个顶点的 X, Y, Z 和颜色值
                float x = readFloatLittleEndian(dataInputStream);
                float y = readFloatLittleEndian(dataInputStream);
                float z = readFloatLittleEndian(dataInputStream);

                // 读取颜色值并规范化为 [0, 1] 范围
                int r = dataInputStream.readByte() & 0xFF;  // 读取字节并转换为 0-255 范围
                int g = dataInputStream.readByte() & 0xFF;
                int b = dataInputStream.readByte() & 0xFF;

                // 将顶点数据和颜色信息添加到列表
                vertices.add(x);
                vertices.add(y);
                vertices.add(z);
                vertices.add(r / 255.0f);  // 红色通道归一化
                vertices.add(g / 255.0f);  // 绿色通道归一化
                vertices.add(b / 255.0f);  // 蓝色通道归一化
            }

            dataInputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load PLY file", Toast.LENGTH_LONG).show();
        }
        return vertices;
    }

    // 辅助方法
    private String readLine(DataInputStream dataInputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = dataInputStream.read()) != -1) {
            if (c == '\n') {
                break;
            }
            sb.append((char) c);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
    private float readFloatLittleEndian(DataInputStream dataInputStream) throws IOException {
        // 读取4个字节
        byte[] bytes = new byte[4];
        dataInputStream.readFully(bytes);

        // 将字节数组从小端转换为大端
        int intBits = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);

        // 将整数转换为 float
        return Float.intBitsToFloat(intBits);
    }
}