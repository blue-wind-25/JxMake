void setup()
{
    pinMode(D13, OUTPUT); // GPIO B.5 Built-in LED

    pinMode(D12, OUTPUT); // GPIO B.4 External LED
    pinMode(D11, OUTPUT); // GPIO B.3 External LED

    pinMode(D10, INPUT ); // GPIO B.2  External SW
}


static bool fast = true;

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        delay(10);
        // Check if the switch is pressed
        if( !digitalRead(D10) ) {
            // Debouncing
            delay(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !digitalRead(D10) );
}


void loop()
{
    digitalWrite(D13, HIGH);

        digitalWrite(D12, HIGH);
        digitalWrite(D11, LOW );
        delayAndCheckSwitch();

        digitalWrite(D12, LOW );
        digitalWrite(D11, HIGH);
        delayAndCheckSwitch();

    digitalWrite(D13, LOW );

        digitalWrite(D12, HIGH);
        digitalWrite(D11, LOW );
        delayAndCheckSwitch();

        digitalWrite(D12, LOW );
        digitalWrite(D11, HIGH);
        delayAndCheckSwitch();
}
