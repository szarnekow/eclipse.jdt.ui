/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.codemanipulation;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaStatusConstants;

/**
 * An implementation of a <code>TextBuffer</code> that is based on <code>ITextSelection</code>
 * and <code>IDocument</code>.
 */
public class TextBuffer {

	private static class DocumentRegion extends TextRegion {
		IRegion fRegion;
		public DocumentRegion(IRegion region) {
			fRegion= region;
		}
		public int getOffset() {
			return fRegion.getOffset();
		}
		public int getLength() {
			return fRegion.getLength();
		}
	}
	
	private IDocument fDocument;
	private PositionUpdater fUpdater;
	
	private static final TextBufferFactory fgFactory= new TextBufferFactory();
	
	TextBuffer(IDocument document) {
		fDocument= document;
		Assert.isNotNull(fDocument);
		fUpdater= new PositionUpdater();
		fDocument.addPositionCategory(fUpdater.CATEGORY);
		fDocument.addPositionUpdater(fUpdater);
	}
	
	/**
	 * Adds the position to the specified position category of the text buffer.
	 * A position that has been added to a position category is updated on each
	 * change applied to the text buffer. Positions may be added multiple times.
	 * The order of the category is maintained.
	 *
	 * @param position the position to be added
	 * @exception CoreException if position describes an invalid range in this text buffer or if
	 *		the category is undefined in this text buffer
	 */
	public void addPosition(TextPosition position) throws CoreException {
		try {
			fDocument.addPosition(fUpdater.CATEGORY, position);
		} catch (BadPositionCategoryException e) {
			Assert.isTrue(false, "Should never happen");			
		} catch (BadLocationException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INDEX_OUT_OF_BOUNDS);
		}
	}
	
	/**
	 * Removes the given position from the text buffer.
	 *
	 * @param position the position to be removed
	 */
	public void removePosition(TextPosition position) {
		try {
			fDocument.removePosition(fUpdater.CATEGORY, position);
		} catch (BadPositionCategoryException e) {
			Assert.isTrue(false, "Should never happen");
		}
	}
	
	/**
	 * Validates if the positions added to this text buffer a valid position ranges. 
	 */
	public boolean validatePositions() {
		try {
			Position[] positions= fDocument.getPositions(PositionUpdater.CATEGORY);
			return fUpdater.canUpdate(positions, getLength());
		} catch (BadPositionCategoryException e) {
			Assert.isTrue(false, "Should never happen");
		}
		return false;
	}

	/**
	 * Returns the number of characters in this text buffer.
	 *
	 * @return the number of characters in this text buffer
	 */
	public int getLength() {
		return fDocument.getLength();
	}
	
	/**
	 * Returns the number of lines in this text buffer.
	 * 
	 * @return the number of lines in this text buffer
	 */
	public int getNumberOfLines() {
		return fDocument.getNumberOfLines();
	}
	
	/**
	 * Returns the character at the given offset in this text buffer.
	 *
	 * @param offset a text buffer offset
	 * @return the character at the offset
	 * @exception  IndexOutOfBoundsException  if the <code>offset</code> 
	 *  argument is negative or not less than the length of this text buffer.
	 */
	public char getChar(int offset) {
		try {
			return fDocument.getChar(offset);
		} catch (BadLocationException e) {
			throw new ArrayIndexOutOfBoundsException(e.getMessage());
		}
	}
	
	/**
	 * Returns the whole content of the text buffer.
	 *
	 * @return the whole content of the text buffer
	 */
	public String getContent() {
		return fDocument.get();
	}
	
	/**
	 * Returns length characters starting from the specified position.
	 *
	 * @return the characters specified by the given text region. Returns <code>
	 *  null</code> if text range is illegal
	 */
	public String getContent(int start, int length) {
		try {
			return fDocument.get(start, length);
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	/**
	 * Returns the preferred line delimiter to be used for this text buffer.
	 * 
	 * @return the preferred line delimiter
	 */
	public String getLineDelimiter() {
		String lineDelimiter= getLineDelimiter(0);
		if (lineDelimiter == null)
			lineDelimiter= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return lineDelimiter;
	}
	
	/**
	 * Returns the line delimiter used for the given line number. Returns <code>
	 * null</code> if the line number is out of range.
	 *
	 * @return the line delimiter used by the given line number or <code>null</code>
	 */
	public String getLineDelimiter(int line) {
		try {
			return fDocument.getLineDelimiter(line);
		} catch (BadLocationException e) {
			return null;
		}	
	}
	
	/**
	 * Returns the line for the given line number. If there isn't any line for
	 * the given line number, <code>null</code> is returned.
	 *
	 * @return the line for the given line number or <code>null</code>
	 */
	public String getLineContent(int line) {
		try {
			IRegion region= fDocument.getLineInformation(line);
			return fDocument.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	/**
	 * Returns the line indent for the given line. If there isn't any line for the
	 * given line number, <code>-1</code> is returned.
	 * 
	 * @return the line indent for the given line number of <code>-1</code>
	 */
	public int getLineIndent(int lineNumber, int tabWidth) {
		String line= getLineContent(lineNumber);
		if (line == null)
			return -1;
		int result= 0;
		int blanks= 0;
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			switch (c) {
				case '\t':
					result++;
					blanks= 0;
					break;
				case	' ':
					blanks++;
					if (blanks == tabWidth) {
						result++;
						blanks= 0;
					}
					break;
				default:
					return result;
			}
		}
		return result;			
	}
	
	/**
	 * Returns a description of the specified line. The line is described by its
	 * offset and its length excluding the line's delimiter. Returns <code>null</code>
	 * if the line doesn't exist.
	 *
	 * @param line the line of interest
	 * @return a line description or <code>null</code> if the given line doesn't
	 *  exist
	 */
	public TextRegion getLineInformation(int line) {
		try {
			return new DocumentRegion(fDocument.getLineInformation(line));
		} catch (BadLocationException e) {
			return null;
		}	
	}
	
	/**
	 * Returns the line number that contains the given position. If there isn't any
	 * line containing the position, -1 is returned.
	 *
	 * @return the line number that contains the given offset or -1 if such line
	 *  doesn't exist
	 */ 
	public TextRegion getLineInformationOfOffset(int offset) {
		try {
			return new DocumentRegion(fDocument.getLineInformationOfOffset(offset));
		} catch (BadLocationException e) {
			return null;
		}	
	}
	
	/**
	 * Returns the line number that contains the given position. If there isn't any
	 * line that contains the position, <code>null</code> is returned. The returned 
	 * string is a copy and doesn't contain the line delimiter.
	 *
	 * @return the line that contains the given offset or <code>null</code> if line
	 *  doesn't exist
	 */ 
	public int getLineOfOffset(int offset) {
		try {
			return fDocument.getLineOfOffset(offset);
		} catch (BadLocationException e) {
			return -1;
		}
	}

	/**
	 * Returns the line that contains the given position. If there isn't any
	 * line that contains the position, <code>null</code> is returned. The returned 
	 * string is a copy and doesn't contain the line delimiter.
	 *
	 * @return the line that contains the given offset or <code>null</code> if line
	 *  doesn't exist
	 */ 
	public String getLineContentOfOffset(int offset) {
		try {
			IRegion region= fDocument.getLineInformationOfOffset(offset);
			return fDocument.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			return null;
		}
	}

	/**
	 * Converts the text determined by the region [offset, length] into an array of lines. 
	 * The lines are copies of the original lines and don't contain any line delimiter 
	 * characters.
	 *
	 * @return the text converted into an array of strings. Returns <code>null</code> if the 
	 *  region lies outside the source. 
	 */
	public String[] convertIntoLines(int offset, int length) {
		try {
			String text= fDocument.get(offset, length);
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(text);
			int size= tracker.getNumberOfLines();
			String result[]= new String[size];
			for (int i= 0; i < size; i++) {
				IRegion region= tracker.getLineInformation(i);
				result[i]= getContent(region.getOffset(), region.getLength());
			}
			return result;
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	/**
	 * Subsitutes the given text for the specified text range. Returns <code>true</code>
	 * if replacing was possible. Otherwise <code>false</code> is returned.
	 *
	 * @param offset the document offset
	 * @param length the length of the specified range
	 * @param text the substitution text
     * @exception  CoreException  if the text range [offset, length] 
     *  is invalid.	 
	 */
	public void replace(int offset, int length, String text) throws CoreException {
		try {
			fDocument.replace(offset, length, text);
		} catch (BadLocationException e) {
			IStatus s= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, 
				"Replace failed due to wrong positions", e);
			throw new CoreException(s);
		}	
	}
	
	/**
	 * Releases this text buffer.
	 */
	/* package */ void release() {
		try {
			fDocument.removePositionUpdater(fUpdater);
			Position[] positions= fDocument.getPositions(PositionUpdater.CATEGORY);
			for (int i= 0; i < positions.length; i++) {
				fDocument.removePosition(PositionUpdater.CATEGORY, positions[i]);
			}
			fDocument.removePositionCategory(fUpdater.CATEGORY);
		} catch (BadPositionCategoryException e) {
			Assert.isTrue(false, "Should never happen");
		}
	}
	
	void setCurrentPositions(TextPosition[] positions) {
		fUpdater.setCurrentPositions(positions);
	}
	
	//---- Factory methods ----------------------------------------------------------------
	
	/**
	 * Acquires a text buffer for the given file. If a text buffer for the given
	 * file already exists, then that one is returned.
	 * 
	 * @param file the file for which a text buffer is requested
	 * @return a managed text buffer for the given file
	 * @exception CoreException if it was not possible to acquire the
	 * 	text buffer
	 */
	public static TextBuffer acquire(IFile file) throws CoreException {
		return fgFactory.acquire(file);
	}
	
	/**
	 * Releases the given text buffer.
	 * 
	 * @param buffer the text buffer to be released
	 */
	public static void release(TextBuffer buffer) {
		fgFactory.release(buffer);
	}

	/**
	 * Commits the changes made to the given text buffer to the underlying
	 * storage system.
	 * 
	 * @param buffer the text buffer containing the changes to be committed.
	 * @param force if <code>true</code> the text buffer is committed in any case.
	 * 	If <code>false</code> the text buffer is <b>ONLY</b> committed if the client 
	 * 	is the last one that holds a reference to the text buffer. Clients of this
	 * 	method must make sure that they don't call this method from within an <code>
	 *  IWorkspaceRunnable</code>.
	 * @param pm the progress monitor used to report progress if committing is
	 * 	necessary
	 */
	public static void commitChanges(TextBuffer buffer, boolean force, IProgressMonitor pm) throws CoreException {
		fgFactory.commitChanges(buffer, force, pm);
	}
	
	/**
	 * Creates a new <code>TextBuffer</code> for the given file. The returned
	 * buffer will not be managed. Any subsequent call to <code>create</code>
	 * with the same file will return a different text buffer.
	 * <p>
	 * If the file is currently open in a text editor, the editors content is copied into
	 * the returned <code>TextBuffer</code>. Otherwise the content is read from
	 * disk.
	 * 
	 * @param file the file for which a text buffer is to be created
	 * @return a new unmanaged text buffer
	 * @exception CoreException if it was not possible to create the text buffer
	 */
	public static TextBuffer create(IFile file) throws CoreException {
		return fgFactory.create(file);
	}
	
	/**
	 * Creates a new <code>TextBuffer</code> for the string. The returned
	 * buffer will not be managed. Any subsequent call to <code>create</code>
	 * with the same string will return a different text buffer.
	 * 
	 * @param content the text buffer's content
	 * @return a new unmanaged text buffer
	 * @exception CoreException if it was not possible to create the text buffer
	 */
	public static TextBuffer create(String content) throws CoreException {
		return fgFactory.create(content);
	}
	
	// Unclear which methods are needed if we get the new save model. If optimal no
	// save is needed at all.
	
	public static void save(TextBuffer buffer, IProgressMonitor pm) throws CoreException {
		fgFactory.save(buffer, pm);
	}
	
	public static void aboutToChange(TextBuffer buffer) throws CoreException {
		fgFactory.aboutToChange(buffer);
	}
	
	public static void changed(TextBuffer buffer) throws CoreException {
		fgFactory.changed(buffer);
	}		
}