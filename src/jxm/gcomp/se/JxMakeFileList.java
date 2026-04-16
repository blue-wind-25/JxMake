/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;

import java.awt.Component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import java.net.URL;

import java.nio.file.attribute.FileTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.function.BiConsumer;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.UndoableEditListener;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import org.fife.ui.rtextarea.RTextScrollPane;

import jxm.*;
import jxm.annotation.*;
import jxm.tool.*;
import jxm.xb.*;


@SuppressWarnings( { "serial", "this-escape" } )
@package_private
public class JxMakeFileList {

    public static class SavedFileState implements Serializable {

        private static final long serialVersionUID = 1L;

        public long dataUpdateTime;
        public int  caretPos;

        public SavedFileState(final long dataUpdateTime_, final int caretPos_)
        {
            dataUpdateTime = dataUpdateTime_;
            caretPos       = caretPos_;
        }

        public SavedFileState()
        { this(0, 0); }

    } // class SavedFileState

    public static class SavedFileStateMap extends HashMap<String, SavedFileState> implements Serializable {

        private static final long serialVersionUID = 1L;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static final String                           _cfgFPath  = JxMakeRootPane.getPath_scriptFileState();

        private static final AppConfigFile<SavedFileStateMap> _globalCfg = new AppConfigFile<>(SavedFileStateMap.class, _cfgFPath);
        private static       SavedFileStateMap                _globalMap = null;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static synchronized SavedFileStateMap _instance()
        {
            if(_cfgFPath != null && _globalMap == null) {
                try {
                    // Try to load first
                    _globalMap = _globalCfg.load();
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                }
                // If load fails, create an empty map
                if(_globalMap == null) _globalMap = new SavedFileStateMap();
                // Ensure the data is saved on exit
                Runtime.getRuntime().addShutdownHook( new Thread( () -> {
                    SavedFileStateMap.saveAll();
                } ) );
            }

            return _globalMap;
        }

        private static synchronized void saveAll()
        {
            if(_globalMap == null) return;

            try {
                // Try to save
                _globalCfg.save(_globalMap);
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static synchronized SavedFileState getSavedFileState(final String absFullFilePath)
        { return _instance().computeIfAbsent( absFullFilePath, k -> new SavedFileState() ); }

        public static synchronized void putSavedFileState(final String absFullFilePath, final FileState fileState)
        {
            final SavedFileState ss = getSavedFileState(absFullFilePath);

            ss.dataUpdateTime = SysUtil.getMS();
            ss.caretPos       = (fileState.textArea != null) ? fileState.textArea.getCaretPosition() : ss.caretPos;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static void writeObjectData(final Object instance, final OutputStreamWriter osw) throws IOException
        {
            osw.write(Texts.ScrEdt_FLDoNotModify);

            for( final Map.Entry<String, SavedFileState> entry : ( (SavedFileStateMap) instance ).entrySet() ) {

                final SavedFileState ss = entry.getValue();

                osw.write( entry.getKey()                    + '\n' );
                osw.write( String.valueOf(ss.dataUpdateTime) + '\n' );
                osw.write( String.valueOf(ss.caretPos      ) + '\n' );
                osw.write(                                     '\n' );

            } // for

            osw.flush();
        }

        public static Object readObjectData(final InputStreamReader isr) throws IOException
        {
            final long              curMS  = SysUtil.getMS();
            final BufferedReader    reader = new BufferedReader(isr);
            final SavedFileStateMap newMap = new SavedFileStateMap();

            /*
            final String strComment1 = reader.readLine(); if(strComment1 == null) return newMap;
            final String strComment2 = reader.readLine(); if(strComment2 == null) return newMap;
            final String strComment3 = reader.readLine(); if(strComment3 == null) return newMap;
            final String strCommentN = reader.readLine(); if(strCommentN == null) return newMap;
            */
            if( !AppConfigFile.skipMarkerLines(reader, Texts.ScrEdt_FLDoNotModify) ) return newMap;

            while(true) {

                final String strAbsFPath  = reader.readLine(); if(strAbsFPath  == null) break;
                final String strUpdTime   = reader.readLine(); if(strUpdTime   == null) break;
                final String strCaretPos  = reader.readLine(); if(strCaretPos  == null) break;
                final String strEmptyLine = reader.readLine(); if(strEmptyLine == null) break;

                // Skip if the entry is too old
                final long updTime  = Long   .valueOf(strUpdTime );
                final int  caretPos = Integer.valueOf(strCaretPos);

                if( curMS - updTime > (365.25 * 24L * 3600L * 1000L) ) continue;

                newMap.put( strAbsFPath, new SavedFileState(updTime, caretPos) );

            } // while

            return newMap;
        }

    } // class SavedFileStateMap

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class FileState {

        public FileTime        lastModifiedTime = null;  // If this field is null, assume the file does not exist
        public boolean         modifiedByOther  = false;
        public boolean         modified         = false;

        public RTextScrollPane textAreaScroll   = null;
        public RSyntaxTextArea textArea         = null;
        public ErrorStrip      errorStrip       = null;

    } // class FileState

    private static final String                     _UNTITLED_FILE       = "\0UNTITLED_FILE\0";

    private static final FileState                  _nullFileState       = new FileState();
    private        final HashMap<String, FileState> _absPathFileStateMap = new HashMap<String, FileState>();

    private synchronized FileState _fileStateForUntitledFile()
    {
        FileState fs = _absPathFileStateMap.get(_UNTITLED_FILE);
        if(fs != null) return fs;

        fs = new FileState();
        fs.lastModifiedTime = _fileGetTimeNow();
        _absPathFileStateMap.put(_UNTITLED_FILE, fs);

        return fs;
    }

    private synchronized FileState _fileStateFor(final String absFullFilePath)
    {
        if( absFullFilePath == null || absFullFilePath.isEmpty() ) return _nullFileState;

        FileState fs = _absPathFileStateMap.get(absFullFilePath);
        if(fs != null) return fs;

        fs = new FileState();
        fs.lastModifiedTime = _pathGetTime(absFullFilePath);
        _absPathFileStateMap.put(absFullFilePath, fs);

        return fs;
    }

    public synchronized void clearAllFileState()
    {
        // Save some part of the file state map
        for( final Map.Entry<String, FileState> entry : _absPathFileStateMap.entrySet() ) {
            SavedFileStateMap.putSavedFileState( entry.getKey(), entry.getValue() );
        }

        SavedFileStateMap.saveAll();

        // Clear the file state map
        _absPathFileStateMap.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static FileTime _fileGetTimeNow()
    { return FileTime.fromMillis( System.currentTimeMillis() ); }

    private static FileTime _pathGetTime(final String path)
    {
        try {
            if( !SysUtil.pathIsValid(path) ) return null;
            return SysUtil.pathGetTime(path);
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return the fallback time
            return ScheduledFileWatcher.FallbackFileTime;
        }
    }

    private static boolean _isModifiedByOther(final String path, final FileTime lastModifiedTime)
    {
        if(lastModifiedTime == null) return true;

        final FileTime curModifiedTime = _pathGetTime(path);

        if(curModifiedTime == null) return true;

        return curModifiedTime.compareTo(lastModifiedTime) > 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static DefaultMutableTreeNode getTreeNodeFrom(final TreePath treePath)
    { return (DefaultMutableTreeNode) ( (treePath == null) ? null : treePath.getLastPathComponent() ); }

    public static FileItem getFileItemNodeFor(final DefaultMutableTreeNode node)
    { return (node == null) ? null : FileItem._fileItemNode(node); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class FileItem {

        /*
        private static final Object CLASS_LOCK = new Object();
        //*/

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private final JxMakeFileList         _ownerFileList;
        private final FileItem               _parentFileItem;
        private final DefaultMutableTreeNode _treeNode;

        private final ScheduledFileWatcher   _sfw;

        private       String                 _incFileName;
        private       String                 _absFullFilePath;
        private       String                 _absDirPath;

        private final ArrayList<FileItem>    _includeFiles = new ArrayList<>();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static FileItem _fileItemNode(final DefaultMutableTreeNode node)
        { return (FileItem) node.getUserObject(); }

        private static TreePath _treePath(final DefaultMutableTreeNode node)
        { return new TreePath( node.getPath() ); }

        private DefaultTreeModel _treeModel()
        { return (DefaultTreeModel) _ownerFileList.tree.getModel(); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public FileItem(final JxMakeFileList ownerFileList)
        {
            _ownerFileList  = ownerFileList;
            _parentFileItem = null;
            _treeNode       = new DefaultMutableTreeNode(null, false);

            _sfw            = null;

            setData(null);
        }

        public FileItem(final JxMakeFileList ownerFileList, final FileItem parentFileItem, final String filePath, final ScheduledFileWatcher sfw)
        {
            _ownerFileList  = ownerFileList;
            _parentFileItem = parentFileItem;
            _treeNode       = new DefaultMutableTreeNode(null, true);

            _sfw            = sfw;

            setData(filePath);
        }

        @Override
        public String toString()
        {
            if( isConsole() ) return Texts.ScrEdt_Console;

            return ( _incFileName != null && !_incFileName.isEmpty() ) ? _incFileName : Texts.ScrEdt_Untitled;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean isConsole()
        { return _parentFileItem == null && !_treeNode.getAllowsChildren(); }

        public boolean isFile()
        { return !isConsole(); }

        public boolean isFileSpecified()
        { return _absFullFilePath != null && !_absFullFilePath.isEmpty(); }

        public boolean isEmpty()
        { return !isFileSpecified() && _includeFiles.isEmpty(); }

        public boolean canHaveState()
        { return _ownerFileList != null && isFileSpecified(); }

        public void clear()
        {
            /*
            synchronized (CLASS_LOCK) {
            //*/

            for(final FileItem fileItem : _includeFiles) {

                final DefaultMutableTreeNode treeNode = fileItem._treeNode;
                if( treeNode.getParent() != null ) _treeModel().removeNodeFromParent(treeNode);

            } // for

            setData(null);

            /*
            } // synchronized
            //*/
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public JxMakeFileList         ownerFileList  () { return _ownerFileList;           }
        public FileItem               parentFileItem () { return _parentFileItem;          }
        public DefaultMutableTreeNode treeNode       () { return _treeNode;                }
        public TreePath               treePath       () { return _treePath(_treeNode);     }
        public FileItem               fileItemNode   () { return _fileItemNode(_treeNode); }

        public String                 incFileName    () { return _incFileName;             }
        public String                 absFullFilePath() { return _absFullFilePath;         }
        public String                 absDirPath     () { return _absDirPath;              }

        public ArrayList<FileItem>    includeFiles   () { return _includeFiles;            }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void setData(final String filePath)
        {
            /*
            synchronized (CLASS_LOCK) {
            //*/

            if(filePath == null) {
                _incFileName     = null;
                _absFullFilePath = null;
                _absDirPath      = null;
            }

            else {
                    _incFileName     = filePath;
                if( SysUtil.pathIsValid(filePath) ) {
                    _absFullFilePath = SysUtil.resolveAbsolutePath(filePath);
                    _absDirPath      = SysUtil.getDirName(_absFullFilePath);
                }
                else {
                    _absFullFilePath = null;
                    _absDirPath      = "";
                }

                final FileItem pfi = parentFileItem();
                if(pfi == null) {
                    _incFileName = SysUtil.resolveRelativePath(_incFileName);
                }
                else {
                    final String adp = parentFileItem()._absDirPath;
                    if( adp != null && _incFileName != null && _incFileName.startsWith(adp) ) {
                        _incFileName = _incFileName.substring( adp.length() );
                        if( _incFileName.startsWith(SysUtil._InternalDirSepStr) ) _incFileName = _incFileName.substring(1);
                    }
                }
            }

            //_ownerFileList._fileStateFor(_absFullFilePath).modified = false;
            _includeFiles.clear();

            _treeNode.setUserObject(this);

            /*
            } // synchronized
            //*/
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void setModified()
        {
            if( !canHaveState() ) return;

            _ownerFileList._fileStateFor(_absFullFilePath).modified = true;
        }

        public void clrModified()
        {
            if( !canHaveState() ) return;

            _ownerFileList._fileStateFor(_absFullFilePath).modified = false;
        }

        public boolean isModified()
        {
            if( !canHaveState() ) return false;

            return _ownerFileList._fileStateFor(_absFullFilePath).modified;
        }

        public boolean isModifiedSS()
        {
            // Check itself
            if( isModified() ) return true;

            // Check the include files recursively
            for(final FileItem fileItem : _includeFiles) {
                if( fileItem.isModifiedSS() ) return true;
            }

            return false;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean isModifiedByOther()
        {
            if( !canHaveState() ) return false;

            return _ownerFileList._fileStateFor(_absFullFilePath).modifiedByOther;
        }

        public boolean isModifiedByOtherSS()
        {
            // Check itself
            if( isModifiedByOther() ) return true;

            // Check the include files recursively
            for(final FileItem fileItem : _includeFiles) {
                if( fileItem.isModifiedByOtherSS() ) return true;
            }

            return false;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private boolean _setClean_impl()
        {
            if(this == _ownerFileList.mainFile && _ownerFileList.mainFileUnsavedModified) {
                _ownerFileList.mainFileUnsavedModified = false;
            }

            if( !canHaveState() ) return false;

            final FileState fileState = _ownerFileList._fileStateFor(_absFullFilePath);

            fileState.modifiedByOther  = false;
            fileState.modified         = false;
            fileState.lastModifiedTime = _pathGetTime(_absFullFilePath);

            return true;
        }

        public void setClean()
        {
            if( !_setClean_impl() ) return;

            _ownerFileList.mainFile._execUserFuncSS( (final FileItem fileItem, final FileState fileState) -> {
                if( fileItem == null || fileItem == this || !_absFullFilePath.equals(fileItem._absFullFilePath) ) return;
                fileItem._setClean_impl();
                fileItem.refreshNodeData();
            } );
        }

        public void setCleanSS()
        {
            // Set itself
            _setClean_impl();

            // Set the include files recursively
            for(final FileItem fileItem : _includeFiles) fileItem.setCleanSS();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void refreshNodeData(final DefaultMutableTreeNode node)
        { _treeModel().nodeChanged(node); }

        public void refreshNodeData()
        { refreshNodeData(_treeNode); }

        public void refreshNodeDataSS(final DefaultMutableTreeNode node)
        {
            // Refresh the current node
            refreshNodeData(node);

            // Recursively refresh all children
            for( int i = 0; i < node.getChildCount(); ++i ) {
                final DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                refreshNodeDataSS(child);
            }
        }

        public void refreshNodeDataSS()
        { refreshNodeDataSS(_treeNode); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private void _refreshNode_impl(final DefaultMutableTreeNode node)
        {
            _treeModel().nodeChanged(node);
            _treeModel().nodeStructureChanged(node);
        }

        public void refreshNode(final DefaultMutableTreeNode node)
        {
            _refreshNode_impl(node);

            _ownerFileList.tree.expandPath( _treePath(node) );
        }

        public void refreshNode()
        { refreshNode(_treeNode); }

        public void refreshNodeSS(final DefaultMutableTreeNode node)
        {
            // Refresh the current node
            refreshNode(node);

            // Recursively refresh all children
            for( int i = 0; i < node.getChildCount(); ++i ) {
                final DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                refreshNodeSS(child);
            }
        }

        public void refreshNodeSS()
        { refreshNodeSS(_treeNode); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static boolean _isRecursiveAdd(final FileItem currentParent, final String absFilePath)
        {
            if(currentParent == null) return false;

            if( currentParent._absFullFilePath.equals(absFilePath) ) return true;

            return _isRecursiveAdd(currentParent._parentFileItem, absFilePath);
        }

        public FileItem _addSubFile_impl(final String absFilePath)
        {
            if( !SysUtil.pathIsValid(absFilePath) ) return null;

            if( _isRecursiveAdd(_parentFileItem, absFilePath) )  return null;

            final FileItem child = new FileItem(_ownerFileList, this, absFilePath, _sfw);

            this._includeFiles.add(child);
            this._treeNode.add(child._treeNode);

            try {
                if(_sfw != null) _sfw.registerFile(child._absFullFilePath, child);
            }
            catch(final IOException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            return child;
        }

        public FileItem addSubFile(final String absFilePath)
        {
            final FileItem child = _addSubFile_impl(absFilePath);

            this.refreshNodeSS();

            return child;
        }

        public void delSubFile(final String filePath)
        {
            if( !SysUtil.pathIsValid(filePath) ) return;

            final String afp = SysUtil.resolveAbsolutePath(filePath);

            for(final FileItem fileItem : this._includeFiles) {

                if( !afp.equals(fileItem._absFullFilePath) ) continue;

                if(_sfw != null) _sfw.unregisterFile(fileItem._absFullFilePath);

                this._includeFiles.remove(fileItem);
                this._treeNode.remove(fileItem._treeNode);

                this.refreshNodeSS();

                return;

            } // for
        }

        private void _delAllSubFiles_impl(final FileItem parent)
        {
            for(final FileItem child : parent._includeFiles) {

                if(_sfw != null) _sfw.unregisterFile(child._absFullFilePath);

                parent._treeNode.remove(child._treeNode);

            } // for

            parent._includeFiles.clear();
        }

        public void delAllSubFiles()
        {
            _delAllSubFiles_impl(this);

            this.refreshNodeSS();
        }

        public FileItem getSubFileFor(final String filePath)
        {
            final String absFilePath = SysUtil.resolvePath(filePath, _absDirPath);

            if( !SysUtil.pathIsValid(absFilePath) ) return null;

            for(final FileItem fileItem : this._includeFiles) {

                if( absFilePath.equals(fileItem._absFullFilePath) ) return fileItem;

            } // for

            return null;
        }

        public FileItem getSubFileFor(final DefaultMutableTreeNode node)
        {
            if(node == this._treeNode) return this;

            for(final FileItem fileItem : _includeFiles) {

                if(fileItem._treeNode == node) return fileItem;

            } // for

            return null;
        }

        public void modSubFiles(final ArrayList<String> relFilePaths)
        {
            // Get the current selection and check if is the same with the tree node for this file item
            final TreePath curSel = _ownerFileList.getSelectedTreePath();

            if( getTreeNodeFrom(curSel) != _treeNode ) return;

            // Fill lists for the file paths and corresponding file items
            final ArrayList<String  > absFilePaths = new ArrayList<>();
            final ArrayList<FileItem> curFileItems = new ArrayList<>();
            final ArrayList<FileItem> newFileItems = new ArrayList<>();

            for(final String relPath : relFilePaths) {

                // Resolve and store the absolute path
                final String absPath = SysUtil.resolvePath(relPath, _absDirPath);
                absFilePaths.add(absPath);

                // Find if the file item for the above absolute path exist
                FileItem fileItem = null;

                for( int i = 0; i < _treeNode.getChildCount(); ++i) {

                    final DefaultMutableTreeNode child = (DefaultMutableTreeNode) _treeNode.getChildAt(i);
                    final FileItem               check = getFileItemNodeFor(child);

                    if( check == null || !absPath.equals(check._absFullFilePath) ) continue;

                    fileItem = check;

                } // for

                // Store the file item (or null if it does not exist)
                curFileItems.add(fileItem);

            } // for

            // Suspend the watching
            if(_sfw != null) {
                for(final FileItem fileItem : curFileItems) {
                    if(fileItem != null) _sfw.suspendFile(fileItem._absFullFilePath);
                }
            }

            // Update the tree
            _delAllSubFiles_impl(this);

            for( int i = 0; i < absFilePaths.size(); ++i ) {

                final String   absPath  = absFilePaths.get(i);
                final FileItem fileItem = curFileItems.get(i);

                if(fileItem == null) {
                    _addSubFile_impl(absPath);
                }
                else {
                    this._includeFiles.add(fileItem);
                    this._treeNode.add(fileItem._treeNode);
                    newFileItems.add(fileItem);
                }

            } // for

            _refreshNode_impl(_treeNode);

            // Restore the selection
            _ownerFileList.expandAllNodes();
            _ownerFileList.selectAndScrollTo(curSel);

            // Resume the watching
            if(_sfw != null) {
                for(final FileItem fileItem : newFileItems) _sfw.resumeFile(fileItem._absFullFilePath);
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static FileItem _recursiveGetParent(final FileItem currentParent, final String absFilePath)
        {
            if(currentParent == null) return null;

            if( currentParent._absFullFilePath.equals(absFilePath) ) return currentParent;

            return _recursiveGetParent(currentParent._parentFileItem, absFilePath);
        }

        public FileItem getParentFileFor(final String filePath)
        {
            final String absFilePath = SysUtil.resolvePath(filePath, _absDirPath);

            return _recursiveGetParent(_parentFileItem, absFilePath);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean saveFileState(final JxMakeRootPane rootPane)
        {
            /*
            if( !isFileSpecified() ) return false;

            final FileState fileState = _ownerFileList._fileStateFor(_absFullFilePath);

            fileState.modifiedByOther  |= _isModifiedByOther(_absFullFilePath, fileState.lastModifiedTime);
            fileState.lastModifiedTime  = _pathGetTime(_absFullFilePath);

            fileState.textArea          = rootPane.textArea          ();
            fileState.textAreaScroll    = rootPane.textAreaScrollPane();
            fileState.errorStrip        = rootPane.errorStrip        ();
            */

            FileState fileState = null;

            if( isFileSpecified() ) {
                fileState                   = _ownerFileList._fileStateFor(_absFullFilePath);
                fileState.modifiedByOther  |= _isModifiedByOther(_absFullFilePath, fileState.lastModifiedTime);
                fileState.lastModifiedTime  = _pathGetTime(_absFullFilePath);
            }
            else {
                fileState                   = _ownerFileList._fileStateForUntitledFile();
                fileState.modifiedByOther   = false;
                fileState.lastModifiedTime  = _fileGetTimeNow();
            }

            fileState.textArea       = rootPane.textArea          ();
            fileState.textAreaScroll = rootPane.textAreaScrollPane();
            fileState.errorStrip     = rootPane.errorStrip        ();

            /*
            SysUtil.stdDbg().printf( "[SAVE] %d %s\n", fileState.textArea.getCaretPosition(), _absFullFilePath );
            //*/

            return true;
        }

// _fileGetTimeNow
//_ownerFileList.()

        public FileState loadFileState(final JxMakeRootPane rootPane)
        {
            /*
            if( !isFileSpecified() ) return null;

            final FileState fileState = _ownerFileList._fileStateFor(_absFullFilePath);

            if(fileState.textArea == null) return null;

            fileState.modifiedByOther |= _isModifiedByOther(_absFullFilePath, fileState.lastModifiedTime);
            */

            FileState fileState = null;

            if( isFileSpecified() ) {
                fileState                   = _ownerFileList._fileStateFor(_absFullFilePath);
                fileState.modifiedByOther  |= _isModifiedByOther(_absFullFilePath, fileState.lastModifiedTime);
            }
            else {
                fileState                   = _ownerFileList._fileStateForUntitledFile();
                fileState.modifiedByOther   = false;
            }

            if(fileState.textArea == null) return null;

            /*
            SysUtil.stdDbg().printf( "[LOAD] %d %s\n", fileState.textArea.getCaretPosition(), _absFullFilePath );
            //*/

            return fileState;
        }

        public FileState getFileState()
        {
            if( !isFileSpecified() ) return null;

            return _ownerFileList._fileStateFor(_absFullFilePath);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private void _execUserFuncSS(final BiConsumer<FileItem, FileState> consumer)
        {
            if( !canHaveState() ) return;

            final FileState fileState = _ownerFileList._fileStateFor(_absFullFilePath);
            consumer.accept(this, fileState);

            for(final FileItem fileItem : _includeFiles) {
                fileItem._execUserFuncSS(consumer);
            }
        }

        public void getAllTextDataSS(final BiConsumer<String, String> consumer)
        {
            //*
            _execUserFuncSS( (final FileItem fileItem, final FileState fileState) -> {
                if(fileState != _nullFileState && fileState.textArea != null) {
                    consumer.accept( fileItem._absFullFilePath, fileState.textArea.getText() );
                }
            } );
            //*/

            /*
            if( !canHaveState() ) return;

            final FileState fileState = _ownerFileList._fileStateFor(_absFullFilePath);
            if(fileState != _nullFileState && fileState.textArea != null) {
                consumer.accept( _absFullFilePath, fileState.textArea.getText() );
            }

            for(final FileItem fileItem : _includeFiles) {
                fileItem.getAllTextDataSS(consumer);
            }
            //*/
        }

    } // class FileItem

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ImageIcon _newImageIcon(final String name)
    { return SysUtil.newImageIcon_defLoc("indicator/" + name); }

    public void expandAllNodes()
    {
        int row = 0;

        while( row < tree.getRowCount() ) {
            tree.expandRow(row);
            row++;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ImageIcon            _consoleIcon;
    private final ImageIcon            _fileIcon_Normal;
    private final ImageIcon            _fileIcon_NormalOther;
    private final ImageIcon            _fileIcon_NormalDel;
    private final ImageIcon            _fileIcon_Modified;
    private final ImageIcon            _fileIcon_ModifiedOther;
    private final ImageIcon            _fileIcon_ModifiedDel;

    private       ScheduledFileWatcher _sfw = null;

    public  final JTree                tree;
    public  final FileItem             console;
    public  final FileItem             mainFile;

    public        boolean              mainFileUnsavedModified;

    public JxMakeFileList(final int fileWatcherPollDelay_MS)
    {
        // Initialize the file watcher
        try {
            _sfw = new ScheduledFileWatcher(fileWatcherPollDelay_MS) {
                @Override
                protected void fileChanged(final String absFilePath, final Object userObject, final FileTime lastModifiedTime, final Reason reason)
                {
                    final FileState fileState = _fileStateFor(absFilePath);
                    if(fileState == null) return;

                    fileState.modifiedByOther  |= true;
                    fileState.lastModifiedTime  = lastModifiedTime;

                    /*
                    SysUtil.stdDbg().printf("fileChanged() '%s' '%s' %s %s\n", absFilePath, userObject, lastModifiedTime, reason);
                    //*/
                }

                @Override
                protected void postFileChanges()
                {
                    mainFile.refreshNodeDataSS();
                    JxMakeFileList.this.expandAllNodes();

                    SwingUtilities.invokeLater(tree::repaint);
                }
            };
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        // Load the icons
        _consoleIcon            = _newImageIcon("console.png"             );
        _fileIcon_Normal        = _newImageIcon("doc_normal.png"          );
        _fileIcon_NormalOther   = _newImageIcon("doc_normal_other.png"    );
        _fileIcon_NormalDel     = _newImageIcon("doc_normal_deleted.png"  );
        _fileIcon_Modified      = _newImageIcon("doc_modified.png"        );
        _fileIcon_ModifiedOther = _newImageIcon("doc_modified_other.png"  );
        _fileIcon_ModifiedDel   = _newImageIcon("doc_modified_deleted.png");

        // Initialize the tree
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("<INVISIBLE_ROOT>");

        tree = new JTree(root);

            tree.setRootVisible(false);
            tree.setShowsRootHandles(true);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

            console  = new FileItem(this                  );
            mainFile = new FileItem(this, null, null, _sfw);

            root.add(console._treeNode);
            root.add(mainFile._treeNode);

        // Use custom icon
        tree.setCellRenderer( new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus)
            {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                /*
                     if(leaf    ) setIcon( super.getLeafIcon  () );
                else if(expanded) setIcon( super.getOpenIcon  () );
                else              setIcon( super.getClosedIcon() );
                */

                if( value == console.treeNode() ) {
                    setIcon(_consoleIcon);
                }
                else if(value == root) {
                    setIcon( super.getLeafIcon() );
                }
                else {
                    final FileItem fileItem = getFileItemNodeFor( (DefaultMutableTreeNode) value );

                    if(fileItem == null) {
                        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                        setIcon( node.isLeaf() ? super.getLeafIcon() : super.getOpenIcon() );
                    }
                    else {

                        if(fileItem == mainFile && mainFileUnsavedModified) {
                            setIcon(_fileIcon_Modified);
                        }

                        else {

                            final FileState fs = fileItem.getFileState();
                            final boolean   mo = fs != null && fs.modifiedByOther;
                            final boolean   md = mo && fs.lastModifiedTime == null;

                            if( fileItem.isModified() ) {
                                setIcon(
                                      md ? _fileIcon_ModifiedDel
                                    : mo ? _fileIcon_ModifiedOther
                                    :      _fileIcon_Modified
                                );
                            }
                            else {
                                setIcon(
                                      md ? _fileIcon_NormalDel
                                    : mo ? _fileIcon_NormalOther
                                    :      _fileIcon_Normal
                                );
                            }
                        }

                    }
                }

                return this;
            }
        } );

        // Prevent collapsing
        tree.addTreeWillExpandListener( new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException
            { /* Allow expansion */ }

            @Override
            public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException
            {
                // Block collapse
                throw new ExpandVetoException(event, "<collapsing is not allowed>");
            }
        } );

        // Prevent double click
        tree.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e)
            {
                //*
                if( !tree.isFocusOwner() ) {
                    tree.requestFocusInWindow();
                    tree.getTopLevelAncestor().requestFocusInWindow();
                }
                //*/

                if( e.getClickCount() == 2 ) e.consume();
            }
        } );
    }

    public void shutdown()
    {
        if(_sfw != null) _sfw.shutdown();

        mainFile.clear();
    }

    public void suspendFileWatcher()
    { if(_sfw != null) _sfw.suspendAll(); }

    public void resumeFileWatcher()
    { if(_sfw != null) _sfw.resumeAll(); }

    public void clearFileWatcher()
    { if(_sfw != null) _sfw.clearEvents(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isEmpty()
    { return mainFile.isEmpty(); }

    public void setMainFile(final String filePath)
    {
        if( _sfw != null && mainFile.isFileSpecified() ) _sfw.unregisterAll();

        mainFile.clear();
        mainFile.setData(filePath);

        try {
             if( _sfw != null && mainFile.isFileSpecified() ) _sfw.registerFile(mainFile._absFullFilePath, mainFile);
        }
        catch(final IOException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        expandAllNodes();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void selectAndScrollTo(final TreePath path)
    {
        // Save the listener(s)
        final TreeSelectionListener[] savedListeners = tree.getTreeSelectionListeners();

        // Remove the listener(s)
        for(final TreeSelectionListener l : savedListeners) tree.removeTreeSelectionListener(l);

        // Change the selection
        tree.expandPath         (path);
        tree.setSelectionPath   (path);
        tree.scrollPathToVisible(path);

        // Restore the listener(s)
        for(final TreeSelectionListener l : savedListeners) tree.addTreeSelectionListener(l);
    }

    public void selectAndScrollTo(final DefaultMutableTreeNode node)
    { selectAndScrollTo( FileItem._treePath(node) ); }

    public void selectAndScrollTo(final FileItem fileItem)
    { selectAndScrollTo( fileItem.treeNode() ); }

    public void selectAndScrollToConsole()
    { selectAndScrollTo(console); }

    public void selectAndScrollToMainFile()
    { selectAndScrollTo(mainFile); }

    public TreePath getSelectedTreePath()
    { return tree.getSelectionPath(); }

    public FileItem getSelectedFileItem()
    {
        final TreePath path = getSelectedTreePath();

        return (path == null) ? null : getFileItemNodeFor( (DefaultMutableTreeNode) path.getLastPathComponent() );
    }

    public SavedFileState getSavedFileState(final String absFullFilePath)
    { return SavedFileStateMap.getSavedFileState(absFullFilePath); }

} // class JxMakeFileList
