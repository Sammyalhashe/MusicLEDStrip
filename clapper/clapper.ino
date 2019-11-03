/*
  Blink

  Turns an LED on for one second, then off for one second, repeatedly.

  Most Arduinos have an on-board LED you can control. On the UNO, MEGA and ZERO
  it is attached to digital pin 13, on MKR1000 on pin 6. LED_BUILTIN is set to
  the correct LED pin independent of which board is used.
  If you want to know what pin the on-board LED is connected to on your Arduino
  model, check the Technical Specs of your board at:
  https://www.arduino.cc/en/Main/Products

  modified 8 May 2014
  by Scott Fitzgerald
  modified 2 Sep 2016
  by Arturo Guadalupi
  modified 8 Sep 2016
  by Colby Newman

  This example code is in the public domain.

  http://www.arduino.cc/en/Tutorial/Blink
*/
//#define DIGITAL_INPUT 3
#define ANALOG A0
#define LED 9
#define DIGITAL_LED 8
#define R_LIGHT 9
#define G_LIGHT 10
#define B_LIGHT 11
#define THRESH 30
boolean trig = true;
boolean state = true;
float analog_sig;

struct color {
    int r;
    int g;
    int b;
};

typedef struct color Color;

Color c;
void setColor(Color *c, int r, int g, int b) {
    c->r = r;
    c->g = g;
    c->b = b;
}

void randomColor(Color *c) {
    setColor(c, random(0, 255), random(0, 255), random(0, 255));
}

void writeRGB(Color c) {
    analogWrite(R_LIGHT, c.r);
    analogWrite(G_LIGHT, c.g);
    analogWrite(B_LIGHT, c.b);
}

// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(ANALOG, INPUT);
  pinMode(DIGITAL_LED, OUTPUT);
  pinMode(R_LIGHT, OUTPUT);
  pinMode(G_LIGHT, OUTPUT);
  pinMode(B_LIGHT, OUTPUT);
    setColor(&c, 0, 0, 0);
//  pinMode(DIGITAL_INPUT, INPUT);
  Serial.begin(9600);
}


// the loop function runs over and over again forever
void loop() {
//  trig = digitalRead(DIGITAL_INPUT);
  analog_sig = analogRead(ANALOG);
  Serial.println(analog_sig);
  // setting rgb values the same for now
  writeRGB(c);
  // if the sensor senses something and
  // it's off initially
  // should turn on
  if (analog_sig >= THRESH) {
    digitalWrite(DIGITAL_LED, HIGH);
    randomColor(&c);
//    analogWrite(LED, analog_sig);
    state = true;
  }
  // if the sensor senses something
  // and it's already on
  // should turn off
  else if (analog_sig < THRESH) {
    digitalWrite(DIGITAL_LED, LOW);
    state = false;
  }
  
   delay(10);
  //digitalWrite(LED_BUILTIN, HIGH);   // turn the LED on (HIGH is the voltage level)
  //delay(1000);                       // wait for a second
  //digitalWrite(LED_BUILTIN, LOW);    // turn the LED off by making the voltage LOW
 // delay(1000);                       // wait for a second
}
