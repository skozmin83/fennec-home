//#include <ESP8266WiFi.h>
//#include <PubSubClient.h>
//
////#define MQTT_VERSION MQTT_VERSION_3_1_1
//
//#define SLEEP_DELAY_IN_SECONDS  10
//#define aref_voltage 3.3         // we tie 3.3V to ARef and measure it with a multimeter!
//
////TMP36 Pin Variables
//int tempPin = A0;        //the analog pin the TMP36's Vout (sense) pin is connected to
////the resolution is 10 mV / degree centigrade with a
////500 mV offset to allow for negative temperatures
//
//unsigned long previousMillis = 0;
//const long interval = 2000;
//const char* ssid = "E7EA3E";
//const char* password = "79296267";
//float temp_f;
////const char* mqtt_server = "192.168.1.6";
////const char* mqtt_username = "";
////const char* mqtt_password = "";
////const char* mqtt_topic = "/58b2389d630689c97e002f36/58b263df630689c97e0031c9/";
//const char* mqtt_server = "api.iostash.io";
//const char* mqtt_username = "58b2389d630689c97e002f36";
//const char* mqtt_password = "Password1";
//const char* mqtt_topic = "/58b2389d630689c97e002f36/58b263df630689c97e0031c9/";
//
//WiFiClient wifiClient;
//PubSubClient mqttClient(wifiClient);
//
//String dataString;
//char payloadBuf[100];
//void setup() {
//  // setup serial port
//  Serial.begin(115200);
//
//  pinMode(A0, INPUT);
//
//  // setup WiFi
//  // setup_wifi();
//  // mqttClient = PubSubClient(serverIp, 1883, callback2, wifiClient);
//  mqttClient.setServer(mqtt_server, 1883);
//  mqttClient.setCallback(callback);
//
//  // Allow the hardware to sort itself out
//  delay(1500);
//}
//
//
//void loop() {
//  if (!mqttClient.connected()) {
//    reconnect();
//  }
//  publishTemp();
//  //disconnect();
//  Serial.println( "Sleeping for a minute");
//  delay(SLEEP_DELAY_IN_SECONDS * 1000);
//  mqttClient.loop();
//}
//
//void publishTemp() {
//  getTemperature();
//  dataString = String("{\"temperature\":") + temp_f + String("}");
//  dataString.toCharArray(payloadBuf, 150);
//  Serial.println(payloadBuf);
//  //boolean rc = client.publish("myTopic", "myMessage");
//  //Serial.println(String("Published result: ") + rc);
//  mqttClient.publish(mqtt_topic, payloadBuf );
//  mqttClient.loop();
//}
//
//void disconnect() {
//  Serial.println( "Closing MQTT connection...");
//  mqttClient.disconnect();
//  mqttClient.loop();
//  Serial.println( "Closing WiFi connection...");
//  WiFi.disconnect();
//}
//
//void reconnect() {
//  WiFi.begin(ssid, password);
//  while (WiFi.status() != WL_CONNECTED) {
//    delay(500);
//    Serial.print(".");
//    setup_wifi();
//  }
//
//  while (!mqttClient.connected()) {
//    Serial.print("Attempting MQTT connection...");
//    //if (client.connect("ESP8266_Client", mqtt_username, mqtt_password)) {
//    //if (client.connect("ESP8266_Client")) {
//    //mqttClient.connect("myClientID", willTopic, willQoS, willRetain, willMessage);
//    //if (mqttClient.connect("client")) {
//    if (mqttClient.connect("ESP8266_Client", mqtt_username, mqtt_password)) {
//      Serial.println("connected");
//    } else {
//      Serial.print("MQTT connection failed, mqtt client state: ");
//      Serial.print(mqttClient.state());
//      Serial.println(", try again in 5 seconds");
//      delay(5000);
//    }
//  }
//}
//
////
////void callback2( char* topic, byte* payload, unsigned int length ) {
////  int i = 0;
////  // creacion del buffer de recepcion
////  // Quito los 2 primeros bytes, Bienen mal, ¿caracteres de longitud?
////  for( i = 2; i < length; i++ ) {
////    message_buff[ i - 2 ] = payload[ i ];
////  }
////  message_buff[ i - 2 ] = '\0';
////
////  String msgString = String( message_buff );
////  Serial.println( msgString );
////}
//
//void callback(char* topic, byte* payload, unsigned int length) {
//  Serial.print("Message arrived [");
//  Serial.print(topic);
//  Serial.print("] ");
//  for (int i = 0; i < length; i++) {
//    Serial.print((char)payload[i]);
//  }
//  Serial.println();
//}
//
//void getTemperature() {
//  unsigned long currentMillis = millis();
//  if (currentMillis - previousMillis >= interval) {
//    previousMillis = currentMillis;
//
//    int tempReading = analogRead(tempPin);
//
//    Serial.print("Temperature raw: ");
//    Serial.print(tempReading);     // the raw analog reading
//
//    // converting that reading to voltage, which is based off the reference voltage
//    float voltage = tempReading * aref_voltage;
//    voltage /= 1024.0;
//
//    // print out the voltage
//    Serial.print(", volts: ");
//    Serial.print(voltage);
//
//    // now print out the temperature
//    temp_f = (voltage - 0.5) * 100 ;  //converting from 10 mv per degree wit 500 mV offset
//    //to degrees ((volatge - 500mV) times 100)
//
//    Serial.print(", celsius: ");
//    Serial.println(temp_f);
//  }
//}
//
//void setup_wifi() {
//  delay(10);
//  Serial.println();
//  Serial.print("Connecting to ");
//  Serial.println(ssid);
//
//  WiFi.begin(ssid, password);
//
//  while (WiFi.status() != WL_CONNECTED) {
//    delay(500);
//    Serial.print(".");
//  }
//
//  Serial.println("");
//  Serial.println("WiFi connected");
//  Serial.println("IP address: ");
//  Serial.println(WiFi.localIP());
//}
