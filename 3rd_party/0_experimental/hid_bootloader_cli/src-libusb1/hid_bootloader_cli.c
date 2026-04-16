/*
 * Modified from the LUFA HID Bootloader by Dean Camera
 *     http://www.lufa-lib.org
 *
 * THIS MODIFIED VERSION IS UNSUPPORTED BY PJRC NOR LUFA.
 *
 * --------------------------------------------------------------------------------
 *
 * Teensy Loader, Command Line Interface
 * Program and Reboot Teensy Board with HalfKay Bootloader
 * http://www.pjrc.com/teensy/loader_cli.html
 * Copyright 2008-2010, PJRC.COM, LLC
 *
 * You may redistribute this program and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software
 * Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 *
 * --------------------------------------------------------------------------------
 *
 * Want to incorporate this code into a proprietary application?
 * Just email paul@pjrc.com to ask. Usually it's not a problem,
 * but you do need to ask to use this code in any way other than
 * those permitted by the GNU General Public License, version 3
 *
 * --------------------------------------------------------------------------------
 *
 * For non-root permissions on ubuntu or similar udev-based linux
 * http://www.pjrc.com/teensy/49-teensy.rules
 */

#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/**************************************************************************************************/

/***** Options (from Command Line Arguments) *****/
static int         wait_for_device_to_appear = 0;
static int         hard_reboot_device        = 0;
static int         reboot_after_programming  = 1;
static int         verbose                   = 0;
static int         code_size                 = 0;
static int         block_size                = 0;
static const char* filename                  = NULL;

/**************************************************************************************************/

/***** Prototypes for USB Access Functions *****/
static int teensy_open();
static int teensy_write(void* buf, int len, double timeout);
static void teensy_close();
static int hard_reboot();

/***** Show Banner *****/
static void show_banner(FILE* stream)
{
    static int banner_shown = 0;
    if(banner_shown) return;
    banner_shown = 1;

    fprintf(stream, "\nHID Bootloader CLI Version 1.0.0");

    fprintf(stream, "\nModified from the original Teensy Loader and LUFA HID Bootloader to work with libusb-1.x.\n");
    fprintf(stream, "\nTHIS MODIFIED VERSION IS UNSUPPORTED BY PJRC NOR LUFA.\n\n");
}

/***** Show Usage *****/
static void show_usage(const char* errMsg)
{
    show_banner(stderr);

    fprintf(stderr, "ERROR: %s!\n\n", errMsg);

    fprintf(stderr, "USAGE:\n");
    fprintf(stderr, "    hid_bootloader_cli -mmcu=<MCU> [-w] [-h] [-n] [-v] <file.hex>\n");
    fprintf(stderr, "        -w : Wait for device to appear\n");
    fprintf(stderr, "        -r : Use hard reboot if device not online\n");
    fprintf(stderr, "        -n : No reboot after programming\n");
    fprintf(stderr, "        -v : Verbose output\n");

    fprintf(stderr, "\n<MCU> = atmegaXXuY or at90usbXXXY\n");

    fprintf(stderr, "\nBased on the TeensyHID command line programmer software:\n");
    fprintf(stderr, "    http://www.pjrc.com/teensy/loader_cli.html\n");

    fprintf(stderr, "\nFor more information, please visit:\n");
    fprintf(stderr, "    http://www.lufa-lib.org\n\n");

    exit(-1);
}

/***** Read Intel Hex File *****/

/*
 * The maximum flash image size we can support chips with larger memory may be used, but only this
 * much intel-hex data can be loaded into memory!
 */
#define MAX_MEMORY_SIZE 0x10000

static unsigned char firmware_image[MAX_MEMORY_SIZE];
static unsigned char firmware_mask [MAX_MEMORY_SIZE];
static int           end_record_seen = 0;
static unsigned int  extended_addr   = 0;
static int           byte_count;
static int           parse_hex_line(char* line);

static int read_intel_hex(const char* filename)
{
    FILE* fp;
    int   i, lineno = 0;
    char  buf[1024];

    byte_count      = 0;
    end_record_seen = 0;
    for(i = 0; i < MAX_MEMORY_SIZE; ++i) {
        firmware_image[i] = 0xFF;
        firmware_mask [i] = 0;
    }
    extended_addr = 0;

    fp = fopen(filename, "r");
    if(fp == NULL) return -1;

    while( !feof(fp) ) {
        *buf = '\0';
        if( !fgets(buf, sizeof(buf), fp) ) break;
        ++lineno;
        if(*buf) {
            if( parse_hex_line(buf) == 0 ) {
                fclose(fp);
                return -2;
            }
        }
        if( end_record_seen ) break;
        if( feof(stdin)     ) break;
    }

    fclose(fp);

    return byte_count;
}

/*
 * From:
 *     https://www.pjrc.com/tech/8051/pm2_docs/intel-hex.html
 *     https://www.pjrc.com/tech/8051/ihex.c
 *
 * Parses a line of intel hex code, stores the data in bytes[] and the beginning address in addr.
 *
 * Returns a 1 if the line was valid, or a 0 if an error occurred.
 *
 * The variable num gets the number of bytes that were stored into bytes[].
 */
static int parse_hex_line(char* line)
{
    int   addr, code, num;
    int   sum, len, cksum, i;
    char* ptr;

    num = 0;

    if( line[0]      != ':' ) return 0;
    if( strlen(line) <  11  ) return 0;
    ptr = line + 1;

    if( !sscanf(ptr, "%02x", &len) ) return 0;
    ptr += 2;

    if( ( (int) strlen(line) ) < ( 11 + (len * 2) ) ) return 0;
    if( !sscanf(ptr, "%04x", &addr)                 ) return 0;
    ptr += 4;

    if( !sscanf(ptr, "%02x", &code) ) return 0;

    if(addr + extended_addr + len >= MAX_MEMORY_SIZE) return 0;
    ptr += 2;
    sum = (len & 255) + ( (addr >> 8) & 255 ) + (addr & 255) + (code & 255);

    if(code != 0) {
        if(code == 1) {
            end_record_seen = 1;
            return 1;
        }
        if(code == 2 && len == 2) {
            if( !sscanf(ptr, "%04x", &i) ) return 1;
            ptr += 4;
            sum += ( (i >> 8) & 255 ) + (i & 255);
            if( !sscanf(ptr, "%02x", &cksum)          ) return 1;
            if( ( (sum & 255) + (cksum & 255) ) & 255 ) return 1;
            extended_addr = i << 4;
        }
        if(code == 4 && len == 2) {
            if( !sscanf(ptr, "%04x", &i)) return 1;
            ptr += 4;
            sum += ( (i >> 8) & 255 ) + (i & 255);
            if( !sscanf(ptr, "%02x", &cksum)          ) return 1;
            if( ( (sum & 255) + (cksum & 255) ) & 255 ) return 1;
            extended_addr = i << 16;
        }
        return 1; /* Non-data line */
    }

    byte_count += len;

    while(num != len) {
        if( sscanf(ptr, "%02x", &i) != 1 ) return 0;
        i &= 255;
        firmware_image[addr + extended_addr + num] = i;
        firmware_mask [addr + extended_addr + num] = 1;
        ptr += 2;
        sum += i;
        ++num;
        if(num >= 256) return 0;
    }

    if( !sscanf(ptr, "%02x", &cksum)          ) return 0;
    if( ( (sum & 255) + (cksum & 255) ) & 255 ) return 0; /* Checksum error */

    return 1;
}

static int ihex_bytes_within_range(int begin, int end)
{
    int i;

    if(begin < 0 || begin >= MAX_MEMORY_SIZE || end < 0 || end >= MAX_MEMORY_SIZE) return 0;

    for(i = begin; i <= end; ++i) {
        if( firmware_mask[i] ) return 1;
    }

    return 0;
}

static void ihex_get_data(int addr, int len, unsigned char* bytes)
{
    int i;

    if(addr < 0 || len < 0 || addr + len >= MAX_MEMORY_SIZE) {
        for(i = 0; i < len; ++i) bytes[i] = 255;
        return;
    }

    for(i = 0; i < len; ++i) {
        if( firmware_mask[addr] ) bytes[i] = firmware_image[addr];
        else                      bytes[i] = 255;
        ++addr;
    }
}

/***** Miscellaneous Functions *****/
static inline void delay(double seconds)
{ usleep(seconds * 1000000.0); }

static void die(const char *str, ...)
{
    va_list ap;

    va_start(ap, str);
    fprintf(stderr, "ERROR: ");
    vfprintf(stderr, str, ap);
    fprintf(stderr, "!\n");
    va_end(ap);

    exit(-1);
}

static int printf_verbose(const char *format, ...)
{
    va_list ap;
    int r = 0;

    va_start(ap, format);
    if(verbose) {
        r = vprintf(format, ap);
        fflush(stdout);
    }
    va_end(ap);

    return r;
}

static void parse_options(int argc, char** argv)
{
    int         i;
    const char* arg;

    for(i = 1; i < argc; ++i) {
        arg = argv[i];
        if(*arg == '-') {
                 if( strcmp (arg, "-w"       ) == 0 ) wait_for_device_to_appear = 1;
            else if( strcmp (arg, "-r"       ) == 0 ) hard_reboot_device        = 1;
            else if( strcmp (arg, "-n"       ) == 0 ) reboot_after_programming  = 0;
            else if( strcmp (arg, "-v"       ) == 0 ) verbose                   = 1;
            else if( strncmp(arg, "-mmcu=", 6) == 0 ) {
                arg += 6;
                     if(strncmp(arg, "at90usb", 7) == 0 ) arg += 7;
                else if(strncmp(arg, "atmega" , 6) == 0 ) arg += 6;
                else                                      die("Unknown MCU type");
                     if( strncmp(arg, "128", 3) == 0 ) { code_size  = 128 * 1024; block_size = 256; }
                else if( strncmp(arg,  "64", 2) == 0 ) { code_size  =  64 * 1024; block_size = 256; }
                else if( strncmp(arg,  "32", 2) == 0 ) { code_size  =  32 * 1024; block_size = 128; }
                else if( strncmp(arg,  "16", 2) == 0 ) { code_size  =  16 * 1024; block_size = 128; }
                else if( strncmp(arg,   "8", 1) == 0 ) { code_size  =   8 * 1024; block_size = 128; }
                else                                   { die("Unknown MCU type");                   }
            }
        }
        else {
            filename = argv[i];
        }
    }
}


/**************************************************************************************************/

/****** Main Program *****/
int main(int argc, char **argv)
{
    unsigned char buf[260];
    int           num, addr, r, first_block = 1, waited = 0;

    show_banner(stdout);

    /* Parse command line arguments */
    parse_options(argc, argv);
    if(!filename ) show_usage("Filename must be specified");
    if(!code_size) show_usage("MCU type must be specified");

    /* Read the intel hex file; this is done first so any error is reported before using USB */
    num = read_intel_hex(filename);
    if(num < 0) die("Error reading Intel HEX file \"%s\"", filename);
    printf_verbose( "Read \"%s\": %d bytes, %.1f%% usage\n", filename, num, ( (double) num ) / ( (double) code_size ) * 100.0 );

    /* Open the USB device */
    while(1) {
        if( teensy_open() ) break;
        if( hard_reboot_device ) {
            if( !hard_reboot() ) die("Unable to find rebootor");
            printf_verbose("Hard reboot performed\n");
            hard_reboot_device        = 0; /* Only hard reboot once */
            wait_for_device_to_appear = 1;
        }
        if(!wait_for_device_to_appear) die("Unable to open device");
        if(!waited) {
            printf_verbose("Waiting for Teensy device ...\n");
            printf_verbose("(hint: press the HWB and/or reset buttons)\n");
            waited = 1;
        }
        delay(0.25);
    }
    printf_verbose("Found HalfKay Bootloader\n");

    /* If we waited for the device, read the hex file again perhaps it changed while we were waiting? */
    if(waited) {
        num = read_intel_hex(filename);
        if(num < 0) die("Error reading Intel HEX file \"%s\"", filename);
        printf_verbose("Read \"%s\": %d bytes, %.1f%% usage\n", filename, num, ( (double) num ) / ( (double) code_size ) * 100.0);
    }

    /* Program the data */
    printf_verbose("Programming ");
    fflush(stdout);
    for(addr = 0; addr < code_size; addr += block_size) {
        if( addr > 0 && !ihex_bytes_within_range(addr, addr + block_size - 1) ) {
            /* Do not waste time on blocks that are unused, but always do the first one to erase the chip */
            continue;
        }
        printf_verbose(".");
        if(code_size < 0x10000) {
            buf[0] =  addr       & 255;
            buf[1] = (addr >> 8) & 255;
        }
        else {
            buf[0] = (addr >>  8) & 255;
            buf[1] = (addr >> 16) & 255;
        }
        ihex_get_data(addr, block_size, buf + 2);
        r = teensy_write(buf, block_size + 2, first_block ? 3.0 : 0.25);
        if(!r) die("Error writing to Teensy");
        first_block = 0;
    }
    printf_verbose("\n");

    /* Reboot to the user's new code */
    if(reboot_after_programming) {
        printf_verbose("Rebooting\n");
        buf[0] = 0xFF;
        buf[1] = 0xFF;
        memset(buf + 2, 0, sizeof(buf) - 2);
        teensy_write(buf, block_size + 2, 0.25);
    }

    teensy_close();

    return 0;
}

/**************************************************************************************************/

/***** USB Access - libusb *****/
#ifdef USE_LIBUSB

/*
 * https://libusb.info
 * https://libusb.sourceforge.io/api-1.0
 *
 * On Windows, if using 'libusb', you will need to replace the driver from 'HidUsb' to 'WinUSB'
 */

#include <libusb-1.0/libusb.h>

static struct libusb_context*       ctx                  = NULL;
static struct libusb_device_handle* libusb_teensy_handle = NULL;

static struct libusb_device_handle* open_usb_device(int vid, int pid)
{
    struct libusb_device**          devs;
    struct libusb_device_descriptor desc;
    struct libusb_device_handle*    handle = NULL;

           ssize_t                  cnt    = 0;
           ssize_t                  idx    = 0;

    if(ctx == NULL) {
        if( libusb_init(&ctx) > 0 ) die("Initialization error");
#if LIBUSB_API_VERSION >= 0x01000106
        libusb_set_option(ctx, LIBUSB_OPTION_LOG_LEVEL, LIBUSB_LOG_LEVEL_WARNING);
#else
        libusb_set_debug(ctx, 3);
#endif
    }

    cnt = libusb_get_device_list(ctx, &devs);

    for(idx = 0; idx < cnt; ++idx) {
        if( libusb_get_device_descriptor(devs[idx], &desc) < 0 ) continue;
        if(desc.idVendor == vid && desc.idProduct == pid) {
            if( libusb_open(devs[idx], &handle) < 0 ) handle = NULL;
            else                                      break;
        }
    }

    if( handle != NULL) {
        if( libusb_kernel_driver_active(handle, 0) >= 0 ) {
            switch( libusb_detach_kernel_driver(handle, 0) ) {
                case LIBUSB_SUCCESS         : break;
                case LIBUSB_ERROR_NOT_FOUND : break;
                default                     : die("Unable detach kernel driver; check USB permissions");
            }
        }
        if( libusb_claim_interface(handle, 0) < 0 ) die("Unable to claim interface; check USB permissions");
    }

    libusb_free_device_list(devs, 1);

    return handle;
}

static int teensy_open()
{
    teensy_close();

                              libusb_teensy_handle = open_usb_device(0x16c0, 0x0478);
    if(!libusb_teensy_handle) libusb_teensy_handle = open_usb_device(0x03eb, 0x2067);

    if(!libusb_teensy_handle) return 0;

    return 1;
}

static int teensy_write(void* buf, int len, double timeout)
{
    int r;

    if(!libusb_teensy_handle) return 0;

    r = libusb_control_transfer(
            libusb_teensy_handle,
            LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE,
            LIBUSB_REQUEST_SET_CONFIGURATION,
            0x0200,
            0,
            buf, len, (int) (timeout * 1000.0)
        );


    if(r < 0) return 0;

    return 1;
}

static void teensy_close()
{
    if(!libusb_teensy_handle) return;

    libusb_close(libusb_teensy_handle);
    libusb_teensy_handle = NULL;

    libusb_exit(ctx);
    ctx = NULL;
}

static int hard_reboot()
{
    struct libusb_device_handle* rebootor;
           int                   r;

                  rebootor = open_usb_device(0x16c0, 0x0477);
    if(!rebootor) rebootor = open_usb_device(0x03eb, 0x2067);

    if(!rebootor) return 0;

    r = libusb_control_transfer(
            rebootor,
            LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE,
            LIBUSB_REQUEST_SET_CONFIGURATION,
            0x0200,
            0,
            (unsigned char*) "reboot", 6, 100
        );

    libusb_close(rebootor);

    if(r < 0) return 0;

    return 1;
}

#endif

/**************************************************************************************************/

/***** USB Access - WIN32 *****/

#ifdef USE_WIN32

/* http://msdn.microsoft.com/en-us/library/ms790932.aspx */

#include <windows.h>

#include <hidclass.h>
#include <hidsdi.h>
#include <setupapi.h>

static HANDLE win32_teensy_handle = NULL;

static HANDLE open_usb_device(int vid, int pid)
{
    GUID                             guid;
    HDEVINFO                         info;
    DWORD                            index, required_size;
    SP_DEVICE_INTERFACE_DATA         iface;
    SP_DEVICE_INTERFACE_DETAIL_DATA* details;
    HIDD_ATTRIBUTES                  attrib;
    HANDLE                           h;
    BOOL                             ret;

    HidD_GetHidGuid(&guid);

    info = SetupDiGetClassDevs(&guid, NULL, NULL, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
    if(info == INVALID_HANDLE_VALUE) return NULL;

    for(index = 0; 1; ++index) {
        iface.cbSize = sizeof(SP_DEVICE_INTERFACE_DATA);
        ret = SetupDiEnumDeviceInterfaces(info, NULL, &guid, index, &iface);
        if(!ret) {
            SetupDiDestroyDeviceInfoList(info);
            break;
        }
        SetupDiGetInterfaceDeviceDetail(info, &iface, NULL, 0, &required_size, NULL);
        details = (SP_DEVICE_INTERFACE_DETAIL_DATA*) malloc(required_size);
        if(details == NULL) continue;
        memset(details, 0, required_size);
        details->cbSize = sizeof(SP_DEVICE_INTERFACE_DETAIL_DATA);
        ret = SetupDiGetDeviceInterfaceDetail(info, &iface, details, required_size, NULL, NULL);
        if(!ret) {
            free(details);
            continue;
        }
        h = CreateFile(details->DevicePath, GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE, NULL, OPEN_EXISTING, FILE_FLAG_OVERLAPPED, NULL);
        free(details);
        if(h == INVALID_HANDLE_VALUE) continue;
        attrib.Size = sizeof(HIDD_ATTRIBUTES);
        ret = HidD_GetAttributes(h, &attrib);
        if(!ret) {
            CloseHandle(h);
            continue;
        }
        if(attrib.VendorID != vid || attrib.ProductID != pid) {
            CloseHandle(h);
            continue;
        }
        SetupDiDestroyDeviceInfoList(info);
        return h;
    }

    return NULL;
}

static int write_usb_device(HANDLE h, void* buf, int len, int timeout)
{
    static        HANDLE event = NULL;
    unsigned char tmpbuf[1040];
    OVERLAPPED    ov;
    DWORD         n, r;

    if( len > sizeof(tmpbuf) - 1 ) return 0;

    if(event == NULL) {
        event = CreateEvent(NULL, TRUE, TRUE, NULL);
        if(!event) return 0;
    }

    ResetEvent(&event);

    memset( &ov, 0, sizeof(ov) );
    ov.hEvent = event;
    tmpbuf[0] = 0;
    memcpy(tmpbuf + 1, buf, len);

    if( !WriteFile(h, tmpbuf, len + 1, NULL, &ov) ) {
        if( GetLastError() != ERROR_IO_PENDING ) return 0;
        r = WaitForSingleObject(event, timeout);
        if(r == WAIT_TIMEOUT) {
            CancelIo(h);
            return 0;
        }
        if(r != WAIT_OBJECT_0) return 0;
    }

    if( !GetOverlappedResult(h, &ov, &n, FALSE) ) return 0;

    return 1;
}

static int teensy_open()
{
    teensy_close();

                             win32_teensy_handle = open_usb_device(0x16c0, 0x0478);
    if(!win32_teensy_handle) win32_teensy_handle = open_usb_device(0x03eb, 0x2067);

    if(!win32_teensy_handle) return 0;

    return 1;
}

static int teensy_write(void* buf, int len, double timeout)
{
    int r;

    if(!win32_teensy_handle) return 0;

    r = write_usb_device( win32_teensy_handle, buf, len, (int) (timeout * 1000.0) );

    return r;
}

static void teensy_close()
{
    if(!win32_teensy_handle) return;

    CloseHandle(win32_teensy_handle);

    win32_teensy_handle = NULL;
}

static int hard_reboot()
{
    HANDLE rebootor;
    int r;

                  rebootor = open_usb_device(0x16c0, 0x0477);
    if(!rebootor) rebootor = open_usb_device(0x03eb, 0x2067);

    if(!rebootor) return 0;

    r = write_usb_device(rebootor, "reboot", 6, 100);

    CloseHandle(rebootor);

    return r;
}

#endif

/**************************************************************************************************/

/***** USB Access - Apple's IOKit, Mac OS-X *****/

#if defined(USE_APPLE_IOKIT)

/* http://developer.apple.com/technotes/tn2007/tn2187.html */

#include <IOKit/IOKitLib.h>
#include <IOKit/hid/IOHIDLib.h>
#include <IOKit/hid/IOHIDDevice.h>

struct usb_list_struct {
           IOHIDDeviceRef   ref;
           int              pid;
           int              vid;
    struct usb_list_struct* next;
};

static struct usb_list_struct* usb_list               = NULL;
static        IOHIDManagerRef  hid_manager            = NULL;

static        IOHIDDeviceRef   iokit_teensy_reference = NULL;

static void attach_callback(void* context, IOReturn r, void* hid_mgr, IOHIDDeviceRef dev)
{
           CFTypeRef        type;
    struct usb_list_struct* n;
    struct usb_list_struct* p;
           int32_t          pid, vid;

    if(!dev) return;

    type = IOHIDDeviceGetProperty( dev, CFSTR(kIOHIDVendorIDKey) );
    if( !type || CFGetTypeID(type) != CFNumberGetTypeID() ) return;
    if( !CFNumberGetValue((CFNumberRef)type, kCFNumberSInt32Type, &vid) ) return;

    type = IOHIDDeviceGetProperty( dev, CFSTR(kIOHIDProductIDKey) );
    if( !type || CFGetTypeID(type) != CFNumberGetTypeID() ) return;
    if( !CFNumberGetValue((CFNumberRef)type, kCFNumberSInt32Type, &pid) ) return;

    n = (struct usb_list_struct*) malloc( sizeof(struct usb_list_struct) );
    if(!n) return;

    n->ref = dev;
    n->vid = vid;
    n->pid = pid;
    n->next = NULL;

    if(usb_list == NULL) {
        usb_list = n;
    }
    else {
        for(p = usb_list; p->next; p = p->next);
        p->next = n;
    }
}

static void detach_callback(void* context, IOReturn r, void* hid_mgr, IOHIDDeviceRef dev)
{
    struct usb_list_struct *p, *tmp, *prev = NULL;

    p = usb_list;

    while(p) {
        if(p->ref == dev) {
            if(prev) prev->next = p->next;
            else     usb_list   = p->next;
            tmp = p;
            p   = p->next;
            free(tmp);
        }
        else {
            prev = p;
            p    = p->next;
        }
    }
}

static void init_hid_manager()
{
    CFMutableDictionaryRef dict;
    IOReturn               ret;

    if(hid_manager) return;

    hid_manager = IOHIDManagerCreate(kCFAllocatorDefault, kIOHIDOptionsTypeNone);
    if( hid_manager == NULL || CFGetTypeID(hid_manager) != IOHIDManagerGetTypeID() ) {
        if(hid_manager) CFRelease(hid_manager);
        die("No HID Manager - maybe this is a pre-Leopard (10.5) system?");
        return;
    }

    dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    if(!dict) return;

    IOHIDManagerSetDeviceMatching(hid_manager, dict);
    CFRelease(dict);
    IOHIDManagerScheduleWithRunLoop(hid_manager, CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);
    IOHIDManagerRegisterDeviceMatchingCallback(hid_manager, attach_callback, NULL);
    IOHIDManagerRegisterDeviceRemovalCallback(hid_manager, detach_callback, NULL);

    ret = IOHIDManagerOpen(hid_manager, kIOHIDOptionsTypeNone);
    if(ret != kIOReturnSuccess) {
        IOHIDManagerUnscheduleFromRunLoop(hid_manager, CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);
        CFRelease(hid_manager);
        die("Error opening HID Manager");
    }
}

static void do_run_loop()
{ while( CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0, true) == kCFRunLoopRunHandledSource ); }

static IOHIDDeviceRef open_usb_device(int vid, int pid)
{
    struct usb_list_struct* p;
           IOReturn         ret;

    init_hid_manager();
    do_run_loop();

    for(p = usb_list; p; p = p->next) {
        if(p->vid == vid && p->pid == pid) {
            ret = IOHIDDeviceOpen(p->ref, kIOHIDOptionsTypeNone);
            if(ret == kIOReturnSuccess) return p->ref;
        }
    }

    return NULL;
}

static void close_usb_device(IOHIDDeviceRef dev)
{
    struct usb_list_struct* p;

    do_run_loop();

    for(p = usb_list; p; p = p->next) {
        if(p->ref == dev) {
            IOHIDDeviceClose(dev, kIOHIDOptionsTypeNone);
            return;
        }
    }
}

static int teensy_open()
{
    teensy_close();

                                iokit_teensy_reference = open_usb_device(0x16c0, 0x0478);
    if(!iokit_teensy_reference) iokit_teensy_reference = open_usb_device(0x03eb, 0x2067);

    if(!iokit_teensy_reference) return 0;

    return 1;
}

static int teensy_write(void* buf, int len, double timeout)
{
    IOReturn ret;

    /*
     * Timeout does not work on OS-X.
     * IOHIDDeviceSetReportWithCallback is not implemented even though Apple documents it with a code example!
     *
     * Submitted to Apple on 22-sep-2009, problem ID 7245050
     */

    if(!iokit_teensy_reference) return 0;

    ret = IOHIDDeviceSetReport(iokit_teensy_reference, kIOHIDReportTypeOutput, 0, buf, len);
    if(ret == kIOReturnSuccess) return 1;

    return 0;
}

static void teensy_close()
{
    if(!iokit_teensy_reference) return;

    close_usb_device(iokit_teensy_reference);
    iokit_teensy_reference = NULL;
}

static int hard_reboot()
{
    IOHIDDeviceRef rebootor;
    IOReturn       ret;

                  rebootor = open_usb_device(0x16c0, 0x0477);
    if(!rebootor) rebootor = open_usb_device(0x03eb, 0x2067);

    if(!rebootor) return 0;

    ret = IOHIDDeviceSetReport(rebootor, kIOHIDReportTypeOutput, 0, (uint8_t*) ("reboot"), 6);

    close_usb_device(rebootor);

    if(ret == kIOReturnSuccess) return 1;

    return 0;
}

#endif
