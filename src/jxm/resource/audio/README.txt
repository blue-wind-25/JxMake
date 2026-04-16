----------------------------------------------------------------------------------------------------
point-smooth-beep.wav
----------------------------------------------------------------------------------------------------
    Original sound file 'point-smooth-beep-230573.mp3'
        Beep, Smooth, Point sound effect. Free for use.
        Download       : https://pixabay.com/sound-effects/point-smooth-beep-230573
        Author profile : https://pixabay.com/users/ribhavagrawal-39286533

    Sound Effect by Ribhav Agrawal from Pixabay
        https://pixabay.com/users/ribhavagrawal-39286533/?utm_source=link-attribution&utm_medium=referral&utm_campaign=music&utm_content=230573
        https://pixabay.com/sound-effects//?utm_source=link-attribution&utm_medium=referral&utm_campaign=music&utm_content=230573

    License information:
        https://pixabay.com/service/license-summary
        https://pixabay.com/service/terms

    Converted using the commands:

        ffmpeg -i ribhavagrawal-point-smooth-beep-230573.mp3                                                                                 \
            -ac 1 -ar 44100 -sample_fmt s16                                                                                                  \
            -af silenceremove=start_periods=1:start_threshold=-50dB:start_duration=0.1:stop_periods=1:stop_threshold=-50dB:stop_duration=0.1 \
            trimmed.wav

        ffmpeg -f lavfi -i anullsrc=r=44100:cl=mono -t 0.25 -sample_fmt s16 silence.wav

        echo -e "file 'silence.wav'\nfile 'trimmed.wav'\nfile 'silence.wav'" | ffmpeg -f concat -safe 0 -i - -c copy point-smooth-beep.wav
----------------------------------------------------------------------------------------------------
