/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Serializable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;

import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.regex.Pattern;

import jxm.*;
import jxm.xb.*;


public class ArduinoBoardsTxt_Dec {

    protected static enum ConfigType {
        Board             ("build:board"                  ),
        Core              ("build:core"                   ),
        Series            ("build:series"                 ),
        Variant           ("build:variant"                ),
        VariantH          ("build:variant_h"              ),

        IncludeDir        (                               ),
        LibraryDir        (                               ),
        CompileMDef       (                               ),
        CompileFlag       (                               ),
        LinkFlag          (                               ),
        LinkSLib          (                               ),

        MCU               ("build:mcu"                    ),
        FPU               ("build:fpu"                    ),
        FloatABI          ("build:float-abi"              ),

        EnableUSB         ("build:enable_usb"             ),
        USBSpeed          ("build:usb_speed"              ),
        USBVID            ("build:usb_vid"                ),
        USBPID            ("build:usb_pid"                ),
        USBManufacturer   ("build:usb_manufacturer"       ),
        USBProduct        ("build:usb_product"            ),
        USBFlags          ("build:usb_flags"              ),

        UploadTool        ("upload:tool"                  ),
        UploadProtocol    ("upload:protocol"              ),
        UploadSpeed       ("upload:speed"                 ),
        UploadMaxSize     ("upload:maximum_size"          ),
        UploadMaxDataSize ("upload:maximum_data_size"     ),
        UploadOptions     ("upload:options"               ),
        ResetMethod       ("upload:resetmethod"           ),
        EraseCmd          ("upload:erase_cmd"             ),
        Use1200BPSTouch   ("upload:use_1200bps_touch"     ),

        FlashFreq         ("build:flash_freq"             ),
        FlashMode         ("build:flash_mode"             ),
        FlashSize         ("build:flash_size"             ),
        FlashOffset       ("build:flash_offset"           ),
        FlashLD           ("build:flash_ld"               ),

        RFCalAddress      ("build:rfcal_addr"             ),
        SPIFFSStart       ("build:spiffs_start"           ),
        SPIFFSEnd         ("build:spiffs_end"             ),
        SPIFFSPageSize    ("build:spiffs_pagesize"        ),
        SPIFFSBlockSize   ("build:spiffs_blocksize"       ),

        Boot              ("build:boot"                   ),
        Boot1             ("build:boot1"                  ),
        Boot2             ("build:boot2"                  ),
        Boot3             ("build:boot3"                  ),
        BootFreq          ("build:boot_freq"              ),
        MemoryType        ("build:memory_type"            ),
        PSRAMType         ("build:psram_type"             ),
        Partition         ("build:partitions"             ),
        Band              ("build:band"                   ),
        LoRaWanPreambleLen("build:LORAWAN_PREAMBLE_LENGTH"),
        LoRaWanDevEUI     ("build:LORAWAN_DEVEUI"         ),
        LoRaWanDebugLevel ("build:LoRaWanDebugLevel"      ),
        JTAGAdapter       (                               ),
        DebugScript       (                               ),

        __UserKey__       (                               ),

        __NotImplemented__(                               )

        ;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final String  _name;

        private ConfigType()
        { _name = '#' + name(); }

        private ConfigType(final String name)
        { _name = name; }

        public String mvName()
        { return _name; }

        public String mvName(final String userConfigName)
        { return (userConfigName != null) ? (_name + userConfigName) : mvName(); }

        @Override
        public String toString()
        { return '{' + _name + '}'; }

        public String toString(final String userConfigName)
        { return (userConfigName != null) ? ('{' + _name + userConfigName + '}') : toString(); }

        public boolean equals(final String userConfigName, final String str)
        { return toString(userConfigName).equals(str); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final int BoardIDMaxLen   = 16;
    protected static final int BoardNameMaxLen = 72;

    protected static final int MenuIDMaxLen    = 64;
    protected static final int MenuTextMaxLen  = 32;
    protected static final int MenuValueMaxLen = 72;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class ConfigItem implements Serializable, Comparable<ConfigItem> {
        private static final long serialVersionUID = SysUtil._SerialVersionUID;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final ConfigType        type;
        public final String            userConfigName;
        public final ArrayList<String> values = new ArrayList<>();
        public final boolean           fromMenu;

        public ConfigItem(final ConfigType type_, final String userConfigName_, final boolean fromMenu_)
        {
            type           = type_;
            userConfigName = userConfigName_;
            fromMenu       = fromMenu_;
        }

        public void finalizeAll()
        { Collections.sort(values, String.CASE_INSENSITIVE_ORDER); }

        public void dump(int level)
        {
            // Print the indent
            _printIndent(++level);

            // Print the name
            SysUtil.stdOut().printf( "%s\n", type.toString(userConfigName) );

            // Print the value(s)
            ++level;
            for(final String value : values) {
                _printIndent(level);
                SysUtil.stdOut().println(value);
            }
        }

        public void dump(final String prefix, final int spaces, final boolean first)
        {
            // Print the name
            if(first) SysUtil.stdOut().printf( "%s", prefix);
            else      _printSpaces( spaces - prefix.length() + 1 );

            SysUtil.stdOut().printf( "%s\n", type.toString(userConfigName) );

            // Print the value(s)
            for(final String value : values) {
                _printSpaces(spaces);
                SysUtil.stdOut().println( XCom.nqStringEllipsis(value, MenuValueMaxLen) );
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public int compareTo(final ConfigItem other)
        {
            int cmp = type.compareTo(other.type);
            if(cmp != 0) return cmp;

            if(userConfigName == null && other.userConfigName != null) return -1;
            if(userConfigName != null && other.userConfigName == null) return  1;
            if(userConfigName != null && other.userConfigName != null) {
                cmp = userConfigName.compareTo(other.userConfigName);
                if(cmp != 0) return cmp;
            }

            cmp = Integer.compare( values.size(), other.values.size() );
            if(cmp != 0) return cmp;

            if(!fromMenu &&  other.fromMenu) return -1;
            if( fromMenu && !other.fromMenu) return  1;
            return 0;
        }
    }

    protected static class ConfigItems extends ArrayList<ConfigItem> {
        private static final long serialVersionUID = SysUtil._SerialVersionUID;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean existByType(final ConfigType type, final String userConfigName, final Boolean fromMenu)
        { return _getByType_impl(type, userConfigName, fromMenu, false) != null; }

        public void deleteByType(final ConfigType type, final String userConfigName, final Boolean fromMenu)
        {
            final ConfigItem configItem = _getByType_impl(type, userConfigName, fromMenu, false);
            if(configItem == null) return;

            remove(configItem);
        }

        public ConfigItem getByType(final ConfigType type, final String userConfigName, final Boolean fromMenu)
        { return _getByType_impl(type, userConfigName, fromMenu, true); }

        private ConfigItem _getByType_impl(final ConfigType type, final String userConfigName, final Boolean fromMenu, final boolean createNewIfNotFound)
        {
            // Search for the item and return it if found
            for(final ConfigItem configItem : this) {
                if(configItem.type == type) {
                    if(configItem.userConfigName == null && userConfigName != null) continue;
                    if(configItem.userConfigName != null && userConfigName == null) continue;
                    if(configItem.userConfigName != null && userConfigName != null) {
                        if( !configItem.userConfigName.equals(userConfigName) ) continue;
                    }
                    if( fromMenu != null && configItem.fromMenu != fromMenu.booleanValue() ) continue;
                    return configItem;
                }
            }

            // Return null here if the 'doNotCreateNewIfNotFound' flag is not set
            if(!createNewIfNotFound) return null;

            // Create a new item
            final ConfigItem configItem = new ConfigItem( type, userConfigName, (fromMenu == null) ? false : fromMenu.booleanValue() );
            add(configItem);

            // Return the new item
            return configItem;
        }

        public void finalizeAll(final boolean doNotSort)
        {
            // Finalize the configuration items
            for(final ConfigItem configItem : this) configItem.finalizeAll();

            // Remove the empty configuration items
            removeIf( (i) -> i.values.isEmpty() );

            // Sort the configuration items
            if( doNotSort || isEmpty() ) return;

            Collections.sort(this);
        }

        public void finalizeAll()
        { finalizeAll(false); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    protected static class MenuIDTextMap extends HashMap<String, String> {}

    protected static class MenuItem implements Serializable, Comparable<MenuItem> {
        private static final long serialVersionUID = SysUtil._SerialVersionUID;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final String      id;                  // This menu's ID
        public       String      text         = null; // This menu's text
        public       String      value        = null; // This menu's value (transient)
        public       ConfigItems configItems  = null; // This menu's configuration items
        public       MenuItems   submenuItems = null; // This menu's submenu items

        public MenuItem(final String id_, final String text_)
        {
            id   = id_;
            text = text_;
        }

        public void addConfigValue(final ConfigType configType, final String userConfigName, final String value)
        {
            if( value == null || value.isEmpty() ) return;

            if(configItems == null) configItems = new ConfigItems();

            final ConfigItem configItem = configItems.getByType(configType, userConfigName, true);

            configItem.values.add( _translateArduinoBuildVar(value) );
        }

        public void addSubMenuItem(final MenuIDTextMap menuIDTextMap, final ArrayList<String> parts, final String value)
        {
            if(submenuItems == null) submenuItems = new MenuItems();

            _addMenuOrSubMenuItem_impl(menuIDTextMap, submenuItems, parts, value);
        }

        public boolean hasText()
        { return text != null; }

        public boolean hasValue()
        { return value != null; }

        public boolean hasConfig()
        { return configItems != null; }

        public boolean hasSubmenu()
        { return submenuItems != null; }

        public void finalizeAll()
        {
            // Error if 'value' and 'text' are both non-null
            if(value != null) {
                if(text != null) {
                    XCom.newJXMFatalLogicError("finalizeAll(): entry '%s' has both non-null 'text' and 'value'", id).printStackTrace();
                    SysUtil.systemExitError();
                }
                text  = value;
                value = null;
            }

            // Finalize the configuration items
            if(configItems != null) configItems.finalizeAll();

            // Finalize the submenu items
            if(submenuItems != null) submenuItems.finalizeAll();
        }

        public void dump(int level)
        {
            // Print the indent
            _printIndent(++level);

            // Adjust the length
            final int _MenuIDMaxLen    = MenuIDMaxLen - _getIndentSize(level);
            final int _MenuTextMaxLen  = MenuTextMaxLen ;
            final int _MenuValueMaxLen = MenuValueMaxLen;

            // Print the menu ID and text
            if( hasText() ) {
                SysUtil.stdOut().printf(
                    "╼%-" + (_MenuIDMaxLen - 1) + "s %-" + _MenuTextMaxLen + "s" ,
                    XCom.nqStringEllipsis(id  , _MenuIDMaxLen - 1),
                    XCom.sqStringEllipsis(text, _MenuTextMaxLen  )
                );
            }
            else {
                SysUtil.stdOut().printf(
                    "%-" + _MenuIDMaxLen + "s %-" + _MenuTextMaxLen + "s" ,
                    XCom.nqStringEllipsis(id  , _MenuIDMaxLen  ),
                    XCom.sqStringEllipsis(text, _MenuTextMaxLen) // It will always produces '<null>'
                );
            }

            // Print the menu value as needed
            if( hasValue() ) {
                SysUtil.stdOut().printf(
                    " = %-" + _MenuValueMaxLen + "s",
                    XCom.sqStringEllipsis(value, _MenuValueMaxLen)
                );
            }

            // Print the menu configuration item(s) as needed
            if( hasConfig() ) {
                boolean first = true;
                for(final ConfigItem configItem : configItems) {
                    configItem.dump(" = ", MenuIDMaxLen + 1 + MenuTextMaxLen + 3 + IndentSize, first);
                    first = false;
                }
            }
            else {
                SysUtil.stdOut().println();
            }

            // Print the menu submenu item(s) as needed
            if( hasSubmenu() ) {
                for(final MenuItem menuItem : submenuItems) menuItem.dump(level);
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public int compareTo(final MenuItem other)
        {
            int cmp;

            if(text == null && other.text != null) return -1;
            if(text != null && other.text == null) return  1;
            if(text != null && other.text != null) {
                if( text.length() != other.text.length() && XCom.isPlainDecInteger(text) && XCom.isPlainDecInteger(other.text) ) {
                    cmp = Integer.compare( text.length(), other.text.length() );
                    if(cmp != 0) return cmp;
                }
                cmp = text.compareToIgnoreCase(other.text);
                if(cmp != 0) return cmp;
            }

            cmp = id.compareTo(other.id);
            if(cmp != 0) return cmp;

            if(value == null && other.value != null) return -1;
            if(value != null && other.value == null) return  1;
            if(value != null && other.value != null) {
                cmp = value.compareTo(other.value);
                if(cmp != 0) return cmp;
            }

            if(configItems == null && other.configItems != null) return -1;
            if(configItems != null && other.configItems == null) return  1;
            if(configItems != null && other.configItems != null) {
                cmp = Integer.compare( configItems.size(), other.configItems.size() );
                if(cmp != 0) return cmp;
            }

            if(submenuItems == null && other.submenuItems != null) return -1;
            if(submenuItems != null && other.submenuItems == null) return  1;
            if(submenuItems != null && other.submenuItems != null) {
                cmp = Integer.compare( submenuItems.size(), other.submenuItems.size() );
                if(cmp != 0) return cmp;
            }

            return 0;
        }
    }

    protected static class MenuItems extends ArrayList<MenuItem> {
        private static final long serialVersionUID = SysUtil._SerialVersionUID;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public int selectedMenuItemIndex = 0;

        public MenuItem getSelectedMenuItem()
        { return isEmpty() ? null : get(selectedMenuItemIndex); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public MenuItem getByID(final MenuIDTextMap menuIDTextMap, final String id)
        {
            // Search for the item and return it if found
            for(final MenuItem menuItem : this) {
                if( menuItem.id.equals(id) ) return menuItem;
            }

            // Create a new item
            final MenuItem menuItem = new MenuItem(id, menuIDTextMap.get(id) );
            add(menuItem);

            // Return the new item
            return menuItem;
        }

        public void finalizeAll(final boolean doNotSort)
        {
            // Finalize the menu items
            for(final MenuItem menuItem : this) menuItem.finalizeAll();

            // Remove the empty menu items
          //removeIf( (i) -> { SysUtil.stdDbg().println("### " + i.id); return ( i.text == null || i.text.isEmpty() ); } );
            removeIf( (i) -> ( i.text == null || i.text.isEmpty() ) );

            // Sort the items while ensuring that the first item will still be selected by default
            if( doNotSort || isEmpty() ) return;

            final String selID = get(0).id;

            Collections.sort(this);

            for(int i = 0; i <  size(); ++i) {
                if( !get(i).id.equals(selID) ) continue;
                selectedMenuItemIndex = i;
                break;
            }
        }

        public void finalizeAll()
        { finalizeAll(false); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class BoardDef implements Serializable, Comparable<BoardDef> {
        private static final long serialVersionUID = SysUtil._SerialVersionUID;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final String      id;                              // Board ID
        public final String      name;                            // Board name
        public final ConfigItems configItems = new ConfigItems(); // Common board              configuration items
        public final MenuItems   menuItems   = new MenuItems  (); // Menu items for selectable configuration items

        public BoardDef(final String id_, final String name_)
        {
            id   = id_;
            name = name_;
        }

        public void addConfigValue(final ConfigType configType, final String userConfigName, final String value, final boolean fromMenu)
        {
            if( value == null || value.isEmpty() ) return;

            final ConfigItem configItem = configItems.getByType(configType, userConfigName, fromMenu);

            configItem.values.add( _translateArduinoBuildVar(value) );
        }

        public void addMenuItem(final MenuIDTextMap menuIDTextMap, final ArrayList<String> parts, final String value)
        { _addMenuOrSubMenuItem_impl(menuIDTextMap, menuItems, parts, value); }

        public void finMenuItem(final ArrayList<String> parts, final ConfigType configType, final String userConfigName, final String value)
        {
            final MenuItem menuItem = _getMenuItem(menuItems, parts);

            if(menuItem == null) return;

            if(menuItem.value != null) {
                menuItem.text  = menuItem.value;
                menuItem.value = null;
            }

            menuItem.addConfigValue(configType, userConfigName, value);
        }

        public void _dispatch(final ArrayList<String> parts, final ConfigType configType, final String userConfigName, final String value)
        {
            // From root
            if(parts == null) {
                final boolean configExists = configItems.existByType(configType, userConfigName, null);
                if(!configExists) addConfigValue(configType, userConfigName, value, false);
            }

            // From menu
            else {
                configItems.deleteByType(configType, userConfigName, false); // Items from menus override items from root
                finMenuItem(parts, configType, userConfigName, value);
            }
        }

        public void finalizeAll()
        {
            configItems.finalizeAll(    );
            menuItems  .finalizeAll(true);
        }

        public void dump()
        {
            // Print the ID and name
            SysUtil.stdOut().printf(
                "[%-" + BoardIDMaxLen + "s] %-" + BoardNameMaxLen + "s\n" ,
                XCom.nqStringEllipsis(id  , BoardIDMaxLen  ),
                XCom.sqStringEllipsis(name, BoardNameMaxLen)
            );

            // Print the configuration item(s) and menu item(s)
            for(final ConfigItem configItem : configItems) configItem.dump(0);
            for(final MenuItem   menuItem   : menuItems  ) menuItem  .dump(0);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public int compareTo(final BoardDef other)
        {
            int cmp;

            cmp = name.compareToIgnoreCase(other.name);
            if(cmp != 0) return cmp;

            cmp = id.compareTo(other.id);
            if(cmp != 0) return cmp;

            cmp = Integer.compare( configItems.size(), other.configItems.size() );
            if(cmp != 0) return cmp;

            cmp = Integer.compare( menuItems.size(), other.menuItems.size() );
            if(cmp != 0) return cmp;

            return 0;
        }
    }

    public static class BoardDefList extends ArrayList<BoardDef> {
        private static final long serialVersionUID = SysUtil._SerialVersionUID;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public int selectedBoardDefIndex = 0;

        public BoardDef getSelectedBoardDef()
        { return isEmpty() ? null : get(selectedBoardDefIndex); }

        public BoardDef getBoardDefByID(final String id)
        {
            for(final BoardDef boardDef : this) {
                if( boardDef.id.equals(id) ) return boardDef;
            }

            return null;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void dump()
        {
            // Dump all the board definitions in the list
            for(final BoardDef boardDef : this) {
                boardDef.dump();
                SysUtil.stdOut().println();
            }
        }

        public BoardDefList finalizeAll()
        {
            // Finalize the board definitions
            for(final BoardDef boardDef : this) boardDef.finalizeAll();

            // Sort the items while ensuring that the first item will still be selected by default
            if( isEmpty() ) return this;

            final String selID = get(0).id;

            Collections.sort(this);

            for(int i = 0; i <  size(); ++i) {
                if( !get(i).id.equals(selID) ) continue;
                selectedBoardDefIndex = i;
                break;
            }

            return this;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public String toEData() throws IOException
        { return SerializableDeepClone.serializeToBase64String(this); }

        public static BoardDefList fromEData(final String str) throws IOException, ClassNotFoundException
        { return (BoardDefList) SerializableDeepClone.deserializeFromBase64String(str); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class KR_Entry {
        private final String[] _from;
        private final String[] _to;

        public KR_Entry(final String[] from, final String[] to)
        {
            _from = from;
            _to   = to;
        }
    }

    private static final ArrayList<KR_Entry> _krList = new ArrayList<>();

    protected static void _execDir_transformKey(final ArrayList<String> key)
    {
        for(final KR_Entry kre : _krList) {

            // Get and check the number of key parts
            final int keySize = key.size();
            if(keySize < kre._from.length) continue;

            // Get the key parts
            final List<String> subList = key.subList(keySize - kre._from.length, keySize);

            // Check if the key parts match
            boolean match = true;
            for(int i = 0; i < kre._from.length; ++i) {
                if( kre._from[i].equals( subList.get(i) ) ) continue;
                match = false;
                break;
            }

            // Replace the key parts match if they match
            if(match) {
                subList.clear();
                subList.addAll( Arrays.asList(kre._to) );
                break;
            }

        } // for
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static interface XKV_Consumer {
        public void apply(final BoardDef boardDef, final ArrayList<String> key, final String value);
    }

    protected static class XKV_Consumer_List {
        final List<XKV_Consumer> list;

        public XKV_Consumer_List(final List<XKV_Consumer> list_)
        { list = list_; }
    }

    protected static class XKV_Handler {
        public final String            part;
        public final XKV_Consumer_List handlers;

        public XKV_Handler(final String part_, final XKV_Consumer... handlers_)
        {
            part     = part_;
            handlers = new XKV_Consumer_List( Arrays.asList(handlers_) );
        }

        public XKV_Handler(final String part_, final List<XKV_Consumer> handlers_)
        {
            part     = part_;
            handlers = new XKV_Consumer_List( handlers_);
        }
    };

    protected static class XKV_Handler_List {
        final List<XKV_Handler> list;

        public XKV_Handler_List(final List<XKV_Handler> list_)
        { list = list_; }
    }

    protected static class XKV_PartHandler {
        public final String           part;
        public final XKV_Handler_List xkvHandlers;

        public XKV_PartHandler(final String part_, final XKV_Handler[] xkvHandlers_)
        {
            part        = part_;
            xkvHandlers = new XKV_Handler_List( Arrays.asList(xkvHandlers_) );
        }

        public XKV_PartHandler(final String part_, final List<XKV_Handler> xkvHandlers_)
        {
            part        = part_;
            xkvHandlers = new XKV_Handler_List( xkvHandlers_ );
        }

        public boolean isEmpty()
        { return xkvHandlers.list.isEmpty(); }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String IndentString = "  ";
    private static final int    IndentSize   = IndentString.length();

    protected static void _printSpaces(final int count)
    { for(int i = 0; i < count; ++i) SysUtil.stdOut().print(" "); }

    protected static void _printIndent(final int level)
    { for(int i = 0; i < level; ++i) SysUtil.stdOut().print(IndentString); }

    protected static int _getIndentSize(final int level)
    { return level * IndentSize; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void _addMenuOrSubMenuItem_impl(final MenuIDTextMap menuIDTextMap, final MenuItems dst, final ArrayList<String> parts, final String value)
    {
        final String   id       = parts.remove(0);
        final MenuItem menuItem = dst.getByID(menuIDTextMap, id);

        if( !parts.isEmpty() ) {
            menuItem.addSubMenuItem(menuIDTextMap, parts, value);
            return;
        }

        menuItem.value = value;
    }

    protected static MenuItem _getMenuItem_impl(final MenuItems menuItems, final ArrayList<String> parts, final int partIdx)
    {

        for(final MenuItem menuItem : menuItems) {
            if( menuItem.id.equals( parts.get(partIdx) ) ) {
                final int partIdxNext = partIdx + 1;
                return ( partIdxNext >= parts.size() ) ? menuItem : _getMenuItem_impl(menuItem.submenuItems, parts, partIdxNext);
            }
        }

        return null;
    }

    protected static MenuItem _getMenuItem(final MenuItems menuItems, final ArrayList<String> parts)
    { return _getMenuItem_impl(menuItems, parts, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static String _readLine(final BufferedReader bfr) throws IOException
    {
        // Combined line string
        String lineStr = "";

        // Loop until a line can be read or EOF
        while(true) {

            // Read one line
            String line = bfr.readLine(); // NOTE : Do not use 'StringBuilder' here because the chance of '\\' appearing would be quite rare

            if(line == null) {
                // Close the file
                bfr.close();
                break;
            }

            line = line.trim();

            // Check if the line is empty
            if( line.isEmpty() ) continue;

            // Check if the last character is the line continuation character,
            // concatenate the line (minus the last character) and continue
            if( line.charAt( line.length() - 1 ) == '\\' ) {
                lineStr += line.substring( 0, line.length() - 1 );
            }
            // Otherwise, concatenate the line and check further
            else {
                // Concatenate the line
                lineStr += line;
                // Return the combined line string if it is not an empty line or a commented line
                if( !lineStr.isEmpty() && lineStr.charAt(0) != '#' ) return lineStr;
                // Otherwise clear the combined line string and continue
                lineStr = "";
            }

        } // while true

        // The file has reached EOF
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private   static final String  UserConfigNamePrefixStr = "@";
    private   static final char    UserConfigNamePrefixChr = '@';

    private   static final Pattern _pmSanitize_String      = Pattern.compile("\\s"                      );
    private   static final Pattern _pmSanitize_SLibFlag    = Pattern.compile("(?:lib)(.+?)(?:\\.a)"     );
    private   static final Pattern _pmSanitize_ULong       = Pattern.compile("(0x[0-9a-fA-F]+|[0-9]+).*");

    private   static final Pattern _pmRestore_String       = Pattern.compile("\\u00A0"                  );

    private   static final Pattern _pmArduinoBuildVar      = Pattern.compile("(\"?\\{[_a-zA-Z0-9]+(?:\\.[_a-zA-Z0-9]+)\\}\"?)"            );
    protected static final Pattern _pmJxMakeABuildVar      = Pattern.compile("(\\{[_a-zA-Z0-9]+(?:@[_a-zA-Z0-9]+)?(?::[_a-zA-Z0-9]+)?\\})");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private   static final BiFunction<String, String, String> _bif_GetValue = (s, v) -> s.replaceAll("%", v);

    private   static final Function  <String,         String> _fun_NOP      = s -> (                              s                          );
    private   static final Function  <String,         String> _fun_StrDesc  = s -> ( _pmSanitize_String  .matcher(s).replaceAll("\u00A0")    );
    private   static final Function  <String,         String> _fun_SLibFlag = s -> ( _pmSanitize_SLibFlag.matcher(s).replaceAll("-l$1"  )    );
    private   static final Function  <String,         String> _fun_ULong    = s -> ( _pmSanitize_ULong   .matcher(s).replaceAll("$1") + "UL" );

    protected static final Function  <String,         String> _res_StrDesc  = s -> ( _pmRestore_String   .matcher(s).replaceAll(" ")         );

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private   static final HashMap  <String, String > _mapTransABV         = new HashMap  <>();
    protected static final ArrayList<XKV_PartHandler> _xkvPartHandlersList = new ArrayList<>();

    private static void _throwErrorInvalidRuleLine(final String ruleFilePath, final String line) throws Exception
    { throw XCom.newException( Texts.EMsg_InvalidRuleLine, (ruleFilePath != null) ? ruleFilePath : SysUtil.DefaultABoardTxtDecRuleFile, line ); }

    protected static String _translateArduinoBuildVar(final String value)
    {
        return XCom.reReplaceTokens( value, _pmArduinoBuildVar, (token) -> {

            final int    chDelCnt = ( token.charAt(0) == '"' ) ? 2 : 1;
            final String bvarName = token.substring( chDelCnt, token.length() - chDelCnt );
            final String result   = _mapTransABV.get(bvarName);

            return (result != null) ? result : token; // If it was not handled above, simply return the original value
        } );
    }

    public static void clearDecRule()
    {
        _mapTransABV        .clear();
        _xkvPartHandlersList.clear();
    }

    public static void loadDecRule() throws IOException
    {
        // Check if the rule is already loaded
        if( !_xkvPartHandlersList.isEmpty() ) return;

        // Find the location of the rule file
        final String ruleFilePath = SysUtil.findArduinoBoardsTxtDecRULESFilePath();

        // Try to load the rule file if it was found
        if(ruleFilePath != null) {
            try {
                // Load the rule file
                _loadDecRule(ruleFilePath);
                // Return here if the rule was successfully loaded
                if( !_xkvPartHandlersList.isEmpty() && !_xkvPartHandlersList.get(0).isEmpty() ) return;
            }
            catch(final IOException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Print message
                SysUtil.stdOut().println();
                SysUtil.stdOut().printf(Texts.EMsg_InvalidRuleLineUserIgn, ruleFilePath);
                SysUtil.stdOut().println();
                // Clear the rule
                clearDecRule();
            }
        }

        // Try to load the default rule
        try {
             _loadDecRule(null);
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Print message
            SysUtil.stdOut().println(Texts.EMsg_InvalidRuleLineDefError);
            // Clear the rule
            clearDecRule();
            // Re-throw the exception
            throw e;
        }
    }

    private static void _loadDecRule(final String ruleFilePath) throws IOException
    {
        try {
            _loadDecRule_impl(ruleFilePath);
        }
        catch(final Exception e) {
            if(ruleFilePath != null) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
            throw XCom.newIOException( e.toString() );
        }
    }

    private static void _loadDecRule_impl(final String ruleFilePath) throws Exception
    {
        // Clear the rule
        clearDecRule();

        // Prepare the handler lists
        final ArrayList<XKV_Handler> hBuild  = new ArrayList<>();
        final ArrayList<XKV_Handler> hUpload = new ArrayList<>();

        // Open the rule file
        final BufferedReader bfr = (ruleFilePath == null)
                                 ? new BufferedReader( new InputStreamReader( JxMake.class.getResourceAsStream(SysUtil.DefaultABoardTxtDecRuleFile), SysUtil._CharEncoding ) )
                                 : new BufferedReader( new InputStreamReader( new FileInputStream(             ruleFilePath                       ), SysUtil._CharEncoding ) );

        /*
         * <Key>.<SubKey>   =   <ConfigType>   <Value>   <Filter>   <Translate>   [, ...]
         *
         * <Key>        : build upload
         *
         * <ConfigType> : <ArduinoBoardsTxt_Dec.ConfigType> except __UserKey__
         *                .<userConfigName>                 for    __UserKey__
         *
         * <SubKey>     : <string>
         *
         * <Filter>     : null StrDesc SLibFlag ULong
         *
         * <Translate>  : -1 = not included for translation
         *                 0 = ""
         *                 1 = ConfigType.toString()
         *                 2 = @{<Key>.<SubKey>}
         */

        // Parse the rule file and build the rule list
        while(true) {

            // Read one line
            final String line = _readLine(bfr);
            if(line == null) break;

            // Tokenize the line
            final String[] tokens = XCom.re_splitWhitespaces(line);

            // Check for '@transform_key' directives
            if( tokens[0].equals("@transform_key") ) {
                // Check the number of tokens
                if(tokens.length != 5) _throwErrorInvalidRuleLine(ruleFilePath, line);
                // Check for the ':' and '->' tokens
                if( !tokens[1].equals(":" ) )  _throwErrorInvalidRuleLine(ruleFilePath, line);
                if( !tokens[3].equals("->") )  _throwErrorInvalidRuleLine(ruleFilePath, line);
                // Extract the source and destination tokens
                final String[] src = tokens[2].split("\\.");
                final String[] dst = tokens[4].split("\\.");
                // Store to the list
                _krList.add( new KR_Entry(src, dst) );
                continue;
            }

            // Check the number of tokens
            if(tokens.length < 6) _throwErrorInvalidRuleLine(ruleFilePath, line);

            // Check for the '=' character
            if( !tokens[1].equals("=") )  _throwErrorInvalidRuleLine(ruleFilePath, line);

            // Split the keys
            final String[] keys = tokens[0].split("\\.");
            if(keys.length != 2) _throwErrorInvalidRuleLine(ruleFilePath, line);

            // Extract the key and subkey
            final String key    = keys[0];
            final String subkey = keys[1];

            // Get the appropriate handler list
            final ArrayList<XKV_Handler> hList = key.equals("build" ) ? hBuild
                                               : key.equals("upload") ? hUpload
                                               : null;
            if(hList == null) _throwErrorInvalidRuleLine(ruleFilePath, line);

            // Process the tokens
            int idx = 2;

            final ArrayList<XKV_Consumer> xkvcList = new ArrayList<>();

            while(true) {

                // Check if there are enough tokens
                final int remCnt = tokens.length - idx;
                if(remCnt < 4) _throwErrorInvalidRuleLine(ruleFilePath, line);

                // Extract the tokens
                final String enumStr   = tokens[idx++];
                final String valueStr  = tokens[idx++];
                final String filterStr = tokens[idx++];
                final String transStr  = tokens[idx++];

                // Process the configuration type
                final ConfigType configType = ( enumStr.charAt(0) == UserConfigNamePrefixChr ) ? ConfigType.__UserKey__ : ConfigType.valueOf(enumStr);

                // Process the filter
                final Function<String, String> filter = filterStr.equals("null"    ) ? _fun_NOP
                                                      : filterStr.equals("StrDesc" ) ? _fun_StrDesc
                                                      : filterStr.equals("SLibFlag") ? _fun_SLibFlag
                                                      : filterStr.equals("ULong"   ) ? _fun_ULong
                                                      : null;
                if(filter == null) _throwErrorInvalidRuleLine(ruleFilePath, line);

                // Process the translation mode
                switch(transStr) {
                    case "-1" :                                                                                                       break;
                    case  "0" :                                          _mapTransABV.put( tokens[0], ""                           ); break;
                    case  "1" : if(configType == ConfigType.__UserKey__) _mapTransABV.put( tokens[0], configType.toString(enumStr) );
                                else                                     _mapTransABV.put( tokens[0], configType.toString(       ) ); break;
                    case  "2" :                                          _mapTransABV.put( tokens[0], "@{" + tokens[0] + '}'       ); break;
                    default   : _throwErrorInvalidRuleLine(ruleFilePath, line);
                }

                // Generate the XKV_Consumer
                if(configType == ConfigType.__NotImplemented__) {
                        xkvcList.add( (b, k, v) -> b._dispatch( k, ConfigType.__NotImplemented__, null   , null                                             ) );
                }
                else {
                    if( enumStr.charAt(0) == UserConfigNamePrefixChr ) {
                        xkvcList.add( (b, k, v) -> b._dispatch( k, configType                   , enumStr, _bif_GetValue.apply( valueStr, filter.apply(v) ) ) );
                    }
                    else {
                        xkvcList.add( (b, k, v) -> b._dispatch( k, configType                   , null   , _bif_GetValue.apply( valueStr, filter.apply(v) ) ) );
                    }
                }

                // Check if there is no more tokens
                if(idx >= tokens.length) break;

                // Check for ',' token
                if( !tokens[idx++].equals(",") ) _throwErrorInvalidRuleLine(ruleFilePath, line);

            } // while true

            // Generate and store the XKV_Handler as needed
            if( !xkvcList.isEmpty() ) {
                hList.add( new XKV_Handler(subkey, xkvcList) );
            }

        } // while true

        // Store all the handlers
        _xkvPartHandlersList.add( new XKV_PartHandler("build" , hBuild ) );
        _xkvPartHandlersList.add( new XKV_PartHandler("upload", hUpload) );
    }

} // class ArduinoBoardsTxt_Dec
