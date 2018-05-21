#include <dht.h>

dht DHT;

#define DHT11_PIN 7
int onoff = LOW;

void setup() {
  pinMode(4, OUTPUT);
  pinMode(8, OUTPUT);
  Serial.begin(115200);
  while (!Serial) { }
  Serial1.begin(115200);
  while (!Serial1) { }
}

void loop() {

  String t;
  while(Serial.available()) {
    t += (char)Serial.read();
  }
  
  while(Serial1.available()) {
    t += (char)Serial1.read();
  }

  if(t.length()) {
    if(t == "temp\r\n" || t == "temp") {
      int chk = DHT.read11(DHT11_PIN);
      String data = "DHT11;" + String((int) DHT.temperature);
             data += ";" + String((int) DHT.humidity) + ";" + onoff;
      Serial1.println(data);
      Serial.println(data);
      blinkLed("blue", 200);
      delay(500);
    }

    else if(t == "changeLed\r\n" || t == "changeLed") {
      onoff = !onoff;
      digitalWrite(4, onoff);
      blinkLed("blue", 200);
    }
    
    else {
      Serial.println("Wrong syntax: " + t);
      Serial1.println("Wrong syntax: " + t);
      blinkLed("blue", 1000);
    }
   }
   delay(20);
}

void blinkLed(String color, int time) {
  if (color == "blue") {
    digitalWrite(8, HIGH);
    delay(time);
    digitalWrite(8, LOW);
  }
  if (color == "red") {
    digitalWrite(4, HIGH);
    delay(time);
    digitalWrite(4, LOW);
  }
}

