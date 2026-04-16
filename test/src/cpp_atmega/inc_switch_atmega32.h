static bool fast = true;

static inline bool __readSwitch()
{ return PIND & _BV(3); }

static void delayAndCheckSwitch()
{
    // Delay and check the switch
    for(char i = 0; i < (fast ? 10 : 50); ++i) {
        // Delay for a while
        _delay_ms(10);
        // Check if the switch is pressed
        if( !__readSwitch() ) {
            // Debouncing
            _delay_ms(100);
            // Change the speed flag and break
            fast = !fast;
            break;
        }
    }

    // Wait until the switch is no longer pressed
    while( !__readSwitch() );
}
