// ~/xsdk/stm32/gcc-arm-none-eabi-10.3-2021.10/bin/arm-none-eabi-objcopy -O ihex STM32WLE5JC.ino.elf STM32WLE5JC.ino.hex


void setup()
{
    pinMode(D9 , OUTPUT); // GPIO A.9  External LED
    pinMode(D26, OUTPUT); // GPIO B.10 External LED

    pinMode(D3 , INPUT ); // GPIO A.3  External SW
}


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay(10);
        // Check if the switch is pressed
        if( !digitalRead(D3) ) {
            // Debouncing
            delay(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !digitalRead(D3) );
}


void loop()
{
    digitalWrite(D9 , HIGH);
    digitalWrite(D26, LOW );
    delayAndCheckSwitch();

    digitalWrite(D9 , LOW );
    digitalWrite(D26, HIGH);
    delayAndCheckSwitch();
}
