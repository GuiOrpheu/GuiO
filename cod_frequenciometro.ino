#include "driver/pcnt.h" // Inclui a biblioteca do contador de pulsos
#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiAP.h>

#define PULSE_PIN 2 // Define o pino para o qual o pulso está conectado

const char* ssid = "IB2"; // Nome da rede
const char* password =  "sergio12"; // Senha da rede
WiFiServer server(80);

pcnt_config_t pcnt_config; // Cria uma instância da estrutura de configuração do contador de pulsos
hw_timer_t * timer1 = NULL; // Cria um ponteiro para o primeiro temporizador de hardware
hw_timer_t * timer2 = NULL; // Cria um ponteiro para o segundo temporizador de hardware
portMUX_TYPE timerMux = portMUX_INITIALIZER_UNLOCKED; // Inicializa o mutex do temporizador
volatile int32_t freq = 0; // Cria uma variável volátil para a frequência
bool flag = true; // Cria uma bandeira booleana

void IRAM_ATTR onTimer1() { // Função de interrupção para o temporizador 1
  portENTER_CRITICAL_ISR(&timerMux); // Entra na seção crítica
  int16_t count; // Cria uma variável para a contagem
  pcnt_get_counter_value(PCNT_UNIT_0, &count); // Obtém o valor do contador
  freq += count; // Adiciona a contagem à frequência
  pcnt_counter_clear(PCNT_UNIT_0); // Limpa o contador
  portEXIT_CRITICAL_ISR(&timerMux); // Sai da seção crítica
}

void IRAM_ATTR onTimer2() { // Função de interrupção para o temporizador 2
  portENTER_CRITICAL_ISR(&timerMux); // Entra na seção crítica
  flag = true; // Define a bandeira como verdadeira
  portEXIT_CRITICAL_ISR(&timerMux); // Sai da seção crítica
}

void setup() { // Função de configuração
  Serial.begin(115200); // Inicia a comunicação serial

  // Configuração do contador de pulsos
  pcnt_config.pulse_gpio_num = PULSE_PIN; // Define o pino do pulso
  pcnt_config.ctrl_gpio_num = PCNT_PIN_NOT_USED; // Define o pino de controle como não usado
  pcnt_config.channel = PCNT_CHANNEL_0; // Define o canal como 0
  pcnt_config.unit = PCNT_UNIT_0; // Define a unidade como 0
  pcnt_config.pos_mode = PCNT_COUNT_INC; // Define o modo positivo como incremento
  pcnt_config.neg_mode = PCNT_COUNT_DIS; // Define o modo negativo como desabilitado
  pcnt_config.lctrl_mode = PCNT_MODE_KEEP; // Mantém o modo anterior quando o sinal LOW é detectado no pino CTRL
  pcnt_config.hctrl_mode = PCNT_MODE_KEEP; // Mantém o modo anterior quando o sinal HIGH é detectado no pino CTRL
  pcnt_config.counter_h_lim = 30000; // Define o limite superior do contador

  // Inicializa o contador de pulsos
  pcnt_unit_config(&pcnt_config);

  // Configura o timer1 para estourar a cada 0,01 segundo (10000 us)
  timer1 = timerBegin(0, 80, true); // Inicia o temporizador 1
  timerAttachInterrupt(timer1, &onTimer1, true); // Anexa a interrupção ao temporizador 1
  timerAlarmWrite(timer1, 10000, true); // Escreve o valor do alarme para o temporizador 1
  timerAlarmEnable(timer1); // Habilita o alarme para o temporizador 1

  // Configura o timer2 para estourar a cada 1 segundo (1000000 us)
  timer2 = timerBegin(1, 80, true); // Inicia o temporizador 2
  timerAttachInterrupt(timer2, &onTimer2, true); // Anexa a interrupção ao temporizador 2
  timerAlarmWrite(timer2, 1000000, true); // Escreve o valor do alarme para o temporizador 2
  timerAlarmEnable(timer2); // Habilita o alarme para o temporizador 2

  if (!WiFi.softAP(ssid, password)) {
    log_e("Soft AP creation failed.");
    while(1);
  }
  IPAddress myIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(myIP);
  server.begin();

  Serial.println("Server started");
}

void loop() { // Função de loop que é executada repetidamente
  WiFiClient client = server.available(); // Verifica se há um cliente disponível
  if (client) { // Se houver um cliente
    while (client.connected()) { // Enquanto o cliente estiver conectado
      if (flag == true){ // Se a bandeira for verdadeira
        flag = false; // Define a bandeira como falsa
        byte* byteArr = (byte*)&freq; // Converte a frequência em um array de bytes
        client.write(byteArr, sizeof(freq)); // Escreve a frequência no cliente
        //Serial.print("Frequência: ");
        //Serial.print(freq);
        //Serial.println("Hz");
        freq = 0; // Redefine a frequência para 0
      }
    }
   client.stop(); // Para a conexão com o cliente
 }
}
