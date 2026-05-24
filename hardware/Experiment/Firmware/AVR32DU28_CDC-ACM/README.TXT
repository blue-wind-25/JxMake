This experiment aims to flatten the AVR DU USB stack and its related SDK to minimize bloat
and optimize speed with the help of Claude Code CLI.

===================================================================================================
===================================================================================================
===================================================================================================

Project goal:
    Strip down the manufacturer USB CDC‑ACM stack to the minimum required functionality, reducing code‑bloat and improving optimization (size and performance).
    The CDC‑ACM stack must remain configurable (single, dual, etc.) and must preserve handling of control signals such as USB_CDC_REQUEST_SET_LINE_CODING, USB_CDC_REQUEST_SET_CONTROL_LINE_STATE, etc.

Workflow:
1. Analyze and propose changes only within the specified directory.
2. After each change, run:
       ./jxmake build
   - If the build succeeds, pause and wait for me to upload/test the firmware. Proceed to the next run only after I confirm runtime behavior is intact.
   - If the build fails, revert the previous changes and add annotations so the same changes will not be attempted again.
3. Be very careful when striping down to raw register/SFR access.
4. If a manufacturer USB stack file is no longer needed or becomes empty, propose its removal.

---------------------------------------------------------------------------------------------------

git status -uno
claude --resume a4c940fe-0f05-438b-86c1-258ace39c0f9

Do not forget to update (from inside hardware/Experiment/MCC_AVRDU_USB_CDC_ACM):
tar -cjvpf ../../../../0_excluded_directory/_0_claude-mem-20260522.tar.gz ~/.claude/ ~/.claude.json

---------------------------------------------------------------------------------------------------

export unc="/opt/uncrustify-0.83.0/bin/uncrustify -c $HOME/Projects/JxMake/tmp/uncrustify.cfg"
export unc="/opt/uncrustify-0.83.0/bin/uncrustify -c $HOME/Projects/JxMake/0_excluded_directory/personal/0-Uncrustify.cfg"


$unc -f inc_usb_avr_du.h | more
$unc -f MCC_AVRDU_USB_CDC_ACM/usb_common/usb_protocol_headers.h | more
$unc -f MCC_AVRDU_USB_CDC_ACM/usb_peripheral/usb_peripheral_avr_du.h | more


find . -type f \( -name "*.cpp" -o -name "*.c" -o -name "*.h" \) -print0 |
while IFS= read -r -d '' f; do
    echo "Processing $f"
    $unc --replace --no-backup "$f"
done


find . -type f \( -name "*.cpp" -o -name "*.c" -o -name "*.h" \) -print0 |
while IFS= read -r -d '' f; do
    echo "Processing $f"
    $unc -f "$f"
done


find . -type f \( -name "*.java" \) -print0 |
while IFS= read -r -d '' f; do
    echo "Processing $f"
    $unc --replace --no-backup "$f"
done

===================================================================================================

Inside the directory pcpp is the original PCPP Python program. Its reference result has already been produced in '/tmp/ref.java' using the command:

python3 pcpp/pcmd.py --line-directive '' --passthru-comments ../../src/jxm/ugc/ARMCortexMThumbC.java.in -o /tmp/ref.java

Inside the directory pcpp_java is the Java translation of Python program. Please fix the compile error in pcpp_java:

make -C pcpp_java

Then ensure it produces the same output as the Python program when executed:

java -jar pcpp_java/target/pcpp-java-1.30.jar --line-directive '' --passthru-comments ../../src/jxm/ugc/ARMCortexMThumbC.java.in -o /tmp/chk.java2

Do not attempt to read /tmp/ref.java and /tmp/chk.java, used diff to check if they are the same or not.


The Java version java.lang.StackOverflowError:

python3 pcpp/pcmd.py --line-directive '' --passthru-comments ../../src/jxm/ugc/ProgSWIM.java.in -o /tmp/ref.java

and then check the Java one:

java -jar pcpp_java/target/pcpp-java-1.30.jar --line-directive '' --passthru-comments ../../src/jxm/ugc/ProgSWIM.java.in -o /tmp/chk.java
