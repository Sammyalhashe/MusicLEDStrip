/**
  This is the code I made for reading input from a (hopefully amplified)
  microphone signal and processing the sound voltage input for detection
  of the beat.
  Upon detection of the music's beat, a color palette and a light pattern will be shown.

  So far, the following color palettes are defined:
  1) Random HSV color pattern
  NOTE: I want to define a color palette based on the frequency of the music.

  So far, the following light patterns are defined:
  1) Each beat detected, a number of LEDs (UPDATE_LEDS), are updated. Each cycle they are shifted over a number (UPDATE_LEDS) pixels.

*/
#include "FastLED.h"
/*
    Globals defined for the script
*/
#define DELAY 10
#define PATTERN_BUTTON 12
#define UPDATE_LEDS 6
#define NUM_LEDS 215
#define LED_TYPE WS2812B
#define BRIGHTNESS 64
#define ANALOG A0
#define THRESH 35
#define DATAPIN 6
#define FACTOR 10 // constant defines how "quickly" a running average "catches up to a new value"

/**
    Variables defined to keep track of the current state
*/
// array holding CRGB values
CRGB leds[NUM_LEDS];
// variable to read in "volume" voltage values
// This value should (hopefully in the future) amplified
// avgVolume is supposed to be a running average to keep track of
// dips in volume. Also, some songs are quieter than others
// I consider a volume of <= 15 effectively zero. I am thus initially
// setting the max volume to 15
float volume;
float maxVol = 15;
float avgVolume = 0;
float prevVolume = 0;
int patternState = LOW;
int prevPatternState = LOW;
int actualButtonState = LOW;
// the average "difference" that would be large enough to cause a trigger
// I'm thinking about using this to set brightness
float avgBump = 0;
int counter = 0; // used for updating the average (number you divide by)
int bumpCounter = 0;
float percentage;
int brightness = BRIGHTNESS;
// keep track of time the program is active for cool timing affects
// right now I'm incrementing it by DELAY
int time = 0;

enum PATTERN {
    SHIFT,
    CENTER_OUT,
    BACKWARDS
};

PATTERN pattern;

// variable to hold current rgb(hsv) value
CRGB crgb = CRGB(0, 0, 0);

// struct defining a color value
struct color {
    int r;
    int g;
    int b;
};
// type alias for clarity
// Also, variable defined to hold current rgb value
typedef struct color Color;
Color c;


/*
    This method takes a pointer to a Color variable
    and procedes to set its values accordingly
*/
void setColor(Color *c, int r, int g, int b) {
    c->r = r;
    c->g = g;
    c->b = b;
}

/*
    This method takes a pointer for a color
    and sets its values to a random rgb value
*/
void randomColor(Color *c) {
    setColor(c, random8(), 255, random8());
}

void setNextColor(Color *c, int palette) {
    switch(palette) {
    case 1 :
        randomColor(c);
        break;
    default       :
        randomColor(c);
        break;
    }
}

/*
    This method takes a color value and
    inserts its details into an equivalent
    CRGB pointer
*/
void crgbFromColor(CRGB *crgb, Color c) {
    // CRGB PURPLE = CHSV( HUE_PURPLE, 255, 255 );
    *crgb = CHSV(c.r, c.g, c.b);
}

void simpleUpdateAverage(float *average, float newVal) {
    float old = *average;
    *average = (old + newVal) / 2.0;
}

void updateAverage(float *average, float newVal, int count) {
    float old = *average;
    *average = old + (newVal - old)/(min(count, FACTOR));
}

void modifyLightArray(CRGB *leds, PATTERN pattern) {
    switch(pattern) {
    case SHIFT:
        // shift the LEDs by UPDATE_LEDS at the beginning of each cycle
        for (int i = NUM_LEDS - 1; i >= UPDATE_LEDS; i--) {
            leds[i] = leds[i - UPDATE_LEDS];
        }
        break;
    case CENTER_OUT:
        /*
        for i in range(0, n // 2 - 1 - UPDATE_LEDS // 2):
            test[i] = test[i + UPDATE_LEDS // 2]
        for i in range(n - UPDATE_LEDS // 2 - 1, n // 2, -1):
            test[i + UPDATE_LEDS // 2] = test[i]
        */
        int NUM_LEDS_HALF = (int) (NUM_LEDS / 2);
        int UPDATE_LEDS_HALF = (int)(UPDATE_LEDS / 2);
        for (int i = 0; i < (int) (NUM_LEDS_HALF - 1 - UPDATE_LEDS_HALF); i++) {
            // center-out left
            leds[i] = leds[i + UPDATE_LEDS_HALF];
        }
        for (int i = NUM_LEDS - UPDATE_LEDS_HALF - 1; i > NUM_LEDS_HALF; i--) {
            // center-out right
            leds[i + UPDATE_LEDS_HALF] = leds[i];
        }
    }

}

// the setup function runs once when you press reset or power the board
void setup() {
    Serial.begin(9600);
    
    /* pattern = CENTER_OUT; */

    // initialize ANALOG pin as pin that reads in microphone signal
    pinMode(ANALOG, INPUT);

    // setup pattern button
    pinMode(PATTERN_BUTTON, INPUT);

    /*
        Using the FastLED library, setup the WS2812B addressable
        LED strip by defining what the LED_TYPE is (WS2812B), the DATAPIN that should talk to
        as well as the number of LEDs on the strip
    */
    FastLED.addLeds<LED_TYPE, DATAPIN>(leds, NUM_LEDS);
    FastLED.setBrightness(brightness);

    // set an initial color value of white
    setColor(&c, 0, 0, 0);

    // load the initial color value into all LEDs
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = CRGB(c.r, c.g, c.b);
    }

    // Light up the strip with new values
    FastLED.show();


    // wait some initial time
    delay(5000);
}



// the loop function runs over and over again forever
void loop() {

    patternState = digitalRead(PATTERN_BUTTON);

    if (patternState == HIGH && prevPatternState == LOW) {
        for (int i = 0; i < NUM_LEDS; i++) {
            leds[i] = CRGB::Black;
        }
        if (actualButtonState == LOW) {
            // toggle on
            actualButtonState = HIGH;
                        
        } else {
            // toggle off
            actualButtonState = LOW;
        }
        
    }
    prevPatternState = patternState;

    if (actualButtonState == HIGH) {
        pattern = SHIFT;
    } else {
        pattern = CENTER_OUT;
    }

    modifyLightArray(leds, pattern);

    // read current volume
    volume = analogRead(ANALOG);
    Serial.println(volume);

    if (volume < avgVolume / 2.0 || volume < 25) {
        volume = 0.0;
    } else {
        // update the average volume only if there is a meaningful volume
        /* counter += 1; */
        /* updateAverage(&avgVolume, volume, counter); */
        simpleUpdateAverage(&avgVolume, volume);
        /* avgVolume = (avgVolume + volume) / 2.0; */
    }

    // update the maximum recorded volume accordingly
    if (volume > maxVol) maxVol = volume;

    // if there is a significant volume change
    // we only consider when the prevVolume is at most at the avgVolume level
    if (volume > THRESH && volume - prevVolume > avgVolume - prevVolume && avgVolume - prevVolume > 0) {
        // update avgBump
        /* bumpCounter += 1; */
        /* updateAverage(&avgBump, volume - prevVolume, bumpCounter); */
        simpleUpdateAverage(&avgBump, volume - prevVolume);
        // calulate the percentage of the avgBump relative to the avgVolume
        // this percentage is then used to translate into a brightness value
        // between 0-255 (basically percentage * 255)
        percentage = avgBump / avgVolume;
        brightness = (int) (percentage * 255);
        FastLED.setBrightness(brightness);
        // write a random rgb(hsv) color into this Color variable
        setNextColor(&c, 1);
        // taking that color, translate it to CRGB value and store it in crgb
        crgbFromColor(&crgb, c);

        switch(pattern) {
            case SHIFT:
                // cycle through UPDATE_LEDS and set their color
                for (int i = 0; i < UPDATE_LEDS; i++) {
                    leds[i] = crgb;
                }        
            case CENTER_OUT:
                int NUM_LEDS_HALF = (int) (NUM_LEDS / 2);
                int UPDATE_LEDS_HALF = (int)(UPDATE_LEDS / 2);
                for (int i = NUM_LEDS_HALF - UPDATE_LEDS_HALF - 1; i <  NUM_LEDS_HALF; i++) {
                    leds[i] = crgb;
                }
                for (int j = NUM_LEDS_HALF; j < NUM_LEDS_HALF + UPDATE_LEDS_HALF; j++) {
                    leds[j] = crgb;
                }
       
        }
        

        // update the changes in the LED strip
        FastLED.show();
    }
    /* else { */
    /*       for (int i = 0; i < UPDATE_LEDS; i++) { */
    /*           leds[i] = CRGB::Black; */
    /*       } */
    /* } */
    // update the changes in the LED strip


    // wait a bit and update time and prevVolume
    delay(DELAY);
    time += DELAY;
    prevVolume = volume;
}
