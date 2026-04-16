// ~/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin/arm-none-eabi-objcopy -O ihex STM32G431CBT6.ino.elf STM32G431CBT6.ino.hex

void setup()
{
    pinMode(D23, OUTPUT      ); // GPIO B.7  External LED
    pinMode(D32, OUTPUT      ); // GPIO C.13 Built-in LED

    pinMode(D25, INPUT_PULLUP); // GPIO B.9  External SW
}


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay(10);
        // Check if the switch is pressed
        if( !digitalRead(D25) ) {
            // Debouncing
            delay(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !digitalRead(D25) );
}


void loop()
{
    digitalWrite(D23, HIGH);
    digitalWrite(D32, HIGH);
    delayAndCheckSwitch();

    digitalWrite(D23, LOW );
    digitalWrite(D32, LOW );
    delayAndCheckSwitch();
}
