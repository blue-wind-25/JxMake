// ~/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin/arm-none-eabi-objcopy -O ihex STM32G070RBT6.ino.elf STM32G070RBT6.ino.hex


void setup()
{
    pinMode(D42, OUTPUT); // GPIO C.10 External LED
    pinMode(D44, OUTPUT); // GPIO C.12 External LED

    pinMode(D33, INPUT ); // GPIO C.1  External SW
}


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay(10);
        // Check if the switch is pressed
        if( !digitalRead(D33) ) {
            // Debouncing
            delay(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !digitalRead(D33) );
}


void loop()
{
    digitalWrite(D42, HIGH);
    digitalWrite(D44, LOW );
    delayAndCheckSwitch();

    digitalWrite(D42, LOW );
    digitalWrite(D44, HIGH);
    delayAndCheckSwitch();
}
