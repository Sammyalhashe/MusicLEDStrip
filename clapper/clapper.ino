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
#include "FastLED.h"
#define DELAY 10
#define UPDATE_LEDS 6
#define NUM_LEDS 200
#define COLOR_ORDER GRB
#define LED_TYPE WS2812B
#define BRIGHTNESS 64
#define ANALOG A0
#define LED 9
#define DIGITAL_LED 8
#define R_LIGHT 9
#define G_LIGHT 10
#define B_LIGHT 11
#define THRESH 40
#define DATAPIN 6
CRGB leds[NUM_LEDS];
boolean trig = true;
boolean state = true;
float analog_sig;
int time = 0;

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
    setColor(c, random8(), 255, random8());
}

void crgbFromColor(CRGB *crgb, Color c) {
    // CRGB PURPLE = CHSV( HUE_PURPLE, 255, 255 );
    *crgb = CHSV(c.r, c.g, c.b);
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
  FastLED.addLeds<LED_TYPE, DATAPIN>(leds, NUM_LEDS);
  FastLED.setBrightness(BRIGHTNESS);

  for (int i = 0; i < NUM_LEDS; i++) {
      leds[i] = CRGB(0, 0, 0);
  }
  FastLED.show();
  delay(1000);
}


CRGB crgb = CRGB(0, 0, 0);
float avgVolume = 0;
float volume;
float prevVolume = 0;
// the loop function runs over and over again forever
void loop() {
  for (int i = NUM_LEDS - 1; i >= UPDATE_LEDS; i--) {
    leds[i] = leds[i - UPDATE_LEDS];
  }
  analog_sig = analogRead(ANALOG);
  volume = analog_sig;
  // update the average volume
  avgVolume = (avgVolume + volume) / 2.0;
  if (volume < avgVolume / 2.0 || volume < 15) {
      volume = 0.0;
  }
  Serial.println(analog_sig);
  // setting rgb values the same for now
  writeRGB(c);
  // if the sensor senses something and
  // it's off initially
  // should turn on
  if (volume > THRESH) {
    digitalWrite(DIGITAL_LED, HIGH);
    randomColor(&c);
//    analogWrite(LED, analog_sig);
    crgbFromColor(&crgb, c);
    state = true;
    for (int i = 0; i < UPDATE_LEDS; i++) {
      leds[i] = crgb;
    }
  }
  FastLED.show();


  // if the sensor senses something
  // and it's already on
  // should turn off
  /* else if (analog_sig < THRESH) { */
  /*   digitalWrite(DIGITAL_LED, LOW); */
  /*   state = false; */
  /* } */
  
   delay(DELAY);
   time += DELAY;
   prevVolume = volume;
}
