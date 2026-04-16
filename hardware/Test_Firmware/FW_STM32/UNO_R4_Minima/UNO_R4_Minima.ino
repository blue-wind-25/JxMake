// ~/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin/arm-none-eabi-objcopy -O ihex UNO_R4_Minima.ino.elf UNO_R4_Minima.ino.hex

void setup()
{
    pinMode(D13, OUTPUT      ); // P111 Built-in LED

    pinMode(D10, OUTPUT      ); // P112 External LED
    pinMode(D11, OUTPUT      ); // P109 External LED

    pinMode(D12, INPUT_PULLUP); // P110 External SW
}


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay(10);
        // Check if the switch is pressed
        if( !digitalRead(D12) ) {
            // Debouncing
            delay(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !digitalRead(D12) );
}


static bool d13 = true;

void loop()
{
    digitalWrite(D10, HIGH);
    digitalWrite(D11, LOW );
    delayAndCheckSwitch();

    digitalWrite(D10, LOW );
    digitalWrite(D11, HIGH);
    delayAndCheckSwitch();

    d13 = !d13;
    digitalWrite(D13, d13 ? HIGH : LOW);
}
