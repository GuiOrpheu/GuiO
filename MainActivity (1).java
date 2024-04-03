package com.example.frequenciometro;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LineChart chart; // Gráfico onde os dados serão exibidos

    private final List<Entry> entries = new ArrayList<>(); // Lista para armazenar os pontos de dados
    private int contadorTempo = 0; // Contador para o eixo X (tempo)
    DecimalFormat timeFormat = new DecimalFormat("#.#"); // Formato para o eixo X (tempo)
    DecimalFormat frequencyFormat = new DecimalFormat("#"); // Formato para o eixo Y (frequência)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.chart); // Encontra o gráfico no layout
        Button startButton = findViewById(R.id.startButton); // Encontra o botão no layout

        Description description = new Description();
        description.setText("Frequencímetro"); // Define o texto da descrição do gráfico
        description.setPosition(200f, 15f); // Define a posição da descrição do gráfico
        chart.setDescription(description); // Define a descrição do gráfico
        chart.getAxisRight().setDrawLabels(false); // Desativa os rótulos do eixo direito

        XAxis xAxis = chart.getXAxis(); // Obtém o eixo X do gráfico
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Define a posição do eixo X para a parte inferior do gráfico
        xAxis.setLabelCount(10); // Define o número de rótulos no eixo X
        xAxis.setGranularity(1f); // Define a granularidade do eixo X
        xAxis.setValueFormatter(new ValueFormatter() { // Define o formatador de valores para o eixo X
            @Override
            public String getFormattedValue(float value) {
                return timeFormat.format(value) + "s"; // Adiciona "s" após cada valor no eixo X
            }
        });

        YAxis yAxis = chart.getAxisLeft(); // Obtém o eixo Y do gráfico
        yAxis.setAxisLineWidth(2f); // Define a largura da linha do eixo Y
        yAxis.setAxisLineColor(Color.BLACK); // Define a cor da linha do eixo Y
        yAxis.setLabelCount(10); // Define o número de rótulos no eixo Y
        yAxis.setValueFormatter(new ValueFormatter() { // Define o formatador de valores para o eixo Y
            @Override
            public String getFormattedValue(float value) {
                return frequencyFormat.format(value) + "Hz"; // Adiciona "Hz" após cada valor no eixo Y
            }
        });

        startButton.setOnClickListener(v -> startDataAcquisition()); // Define o que acontece quando o botão é clicado
    }

    private void startDataAcquisition() {
        new Thread(() -> { // Inicia uma nova thread
            try {
                Socket socket = new Socket("192.168.4.1", 80); // Conecta ao servidor
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream()); // Cria um fluxo de entrada de dados

                while (true) { // Loop infinito
                    byte[] byteArr = new byte[4]; // Cria um array de bytes
                    dataInputStream.readFully(byteArr); // Lê 4 bytes do fluxo de entrada
                    final int freq = ByteBuffer.wrap(byteArr).order(ByteOrder.LITTLE_ENDIAN).getInt(); // Converte os bytes em um inteiro
                    runOnUiThread(() -> updateChart(freq)); // Atualiza o gráfico na thread da interface do usuário
                }
            } catch (IOException e) {
                e.printStackTrace(); // Imprime a pilha de chamadas se ocorrer uma exceção
            }
        }).start(); // Inicia a thread
    }

    private void updateChart(int freq) { // Método para atualizar o gráfico

        contadorTempo++; // Incrementa o contador de tempo

        entries.add(new Entry(contadorTempo, freq)); // Adiciona um novo ponto de dados

        if (contadorTempo > 10) { // Se o contador de tempo for maior que 10
            chart.getXAxis().setAxisMinimum(contadorTempo - 10); // Ajusta o valor mínimo do eixo X
            chart.getXAxis().setAxisMaximum(contadorTempo); // Ajusta o valor máximo do eixo X
        }

        LineDataSet dataSet = new LineDataSet(entries, "Frequência"); // Cria um conjunto de dados
        dataSet.setColor(Color.BLUE); // Define a cor da linha
        dataSet.setValueTextSize(10f); // Define o tamanho do texto dos valores

        LineData lineData = new LineData(dataSet); // Cria os dados da linha
        chart.setData(lineData); // Define os dados do gráfico

        chart.invalidate(); // Atualiza o gráfico
    }
}

