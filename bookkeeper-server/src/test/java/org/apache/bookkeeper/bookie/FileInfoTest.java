package org.apache.bookkeeper.bookie;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(value = Parameterized.class)
public class FileInfoTest {
    private FileInfo fileInfo;
    
    private File newFile;
    private long size;
    private expectedOutputType expectedOutput;
    private expectExceptionType expectException;
    
    private File currentFile;
    private byte[] masterKey;
    private int fileInfoVersion;
    
    private File newLocationFile;
    
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String CURRENT_FILE_PATH = "src/test/resources/fileinfo/current_file.txt";
    private static final String NEW_LOCATION_FILE_PATH = "src/test/resources/fileinfo/new_location_file.txt";
    
    private static long CURRENT_FILE_SIZE = 1024;
    private static long NEW_FILE_SIZE = 0;
    
    public FileInfoTest(File newFile, long size, expectedOutputType expectedOutput, expectExceptionType expectException) {
        this.newFile = newFile;
        this.size = size;
        this.expectedOutput = expectedOutput;
        this.expectException = expectException;
    }
    
    private enum newFileType {
        NOT_EXISTING_FILE,
        EXISTING_FILE;
        
        public File getNewFileType() {
            switch (this) {
                case NOT_EXISTING_FILE:
                    return null;
                case EXISTING_FILE:
                    return new File(USER_DIR, "src/test/resources/fileinfo/new_location_file.txt");
                default:
                    return null;
            }
        }
    }
    
    private enum sizeType {
        NEGATIVE_SIZE,
        ZERO_SIZE,
        LONG_MAX_VALUE,
        OVER_CURRENT_FILE_SIZE,
        UNDER_CURRENT_FILE_SIZE;
        
        public long getSizeType() {
            switch (this) {
                case NEGATIVE_SIZE:
                    return -1L;
                case ZERO_SIZE:
                    return 0;
                case OVER_CURRENT_FILE_SIZE:
                    return CURRENT_FILE_SIZE + 1;
                case UNDER_CURRENT_FILE_SIZE:
                    return CURRENT_FILE_SIZE - 1;
                default:
                    return Long.MAX_VALUE;
            }
        }
    }
    
    private enum expectedOutputType {
        NO_CONTENT_COPIED,
        PARTIAL_CONTENT_COPIED,
        ALL_CONTENT_COPIED;
    }
    
    private enum expectExceptionType {
        YES,
        NO
    }
    
    @Before
    public void setUp() throws IOException {
        currentFile = new File(USER_DIR, CURRENT_FILE_PATH);
        currentFile.createNewFile();
        
        masterKey = new byte[]{};
        fileInfoVersion = 1;
        
        try (FileOutputStream fos = new FileOutputStream(currentFile)) {
            ByteBuffer bb = ByteBuffer.allocate((int) CURRENT_FILE_SIZE);
            bb.putInt(FileInfo.SIGNATURE);
            bb.putInt(FileInfo.CURRENT_HEADER_VERSION);
            bb.putInt(masterKey.length);
            bb.put(masterKey);
            bb.putInt(0);
            bb.flip();
            
            fos.write(bb.array(), 0, (int) CURRENT_FILE_SIZE);
        }
        
        fileInfo = new FileInfo(currentFile, masterKey, fileInfoVersion);
        
        newLocationFile = new File(USER_DIR, NEW_LOCATION_FILE_PATH);
        newLocationFile.createNewFile();
    }
    
    @After
    public void tearDown() {
        if (currentFile != null && currentFile.exists())
            currentFile.delete();
        
        if (newLocationFile != null && newLocationFile.exists())
            newLocationFile.delete();
        
        NEW_FILE_SIZE = 0;
    }
    
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, sizeType.ZERO_SIZE.getSizeType(), null, expectExceptionType.YES},
                {newFileType.NOT_EXISTING_FILE.getNewFileType(), sizeType.LONG_MAX_VALUE.getSizeType(), null, expectExceptionType.YES},
                {newFileType.EXISTING_FILE.getNewFileType(), sizeType.LONG_MAX_VALUE.getSizeType(), expectedOutputType.ALL_CONTENT_COPIED, expectExceptionType.NO},
                {newFileType.EXISTING_FILE.getNewFileType(), sizeType.OVER_CURRENT_FILE_SIZE.getSizeType(), expectedOutputType.ALL_CONTENT_COPIED, expectExceptionType.NO},
                {newFileType.EXISTING_FILE.getNewFileType(), sizeType.UNDER_CURRENT_FILE_SIZE.getSizeType(), expectedOutputType.PARTIAL_CONTENT_COPIED, expectExceptionType.NO},
                {newFileType.EXISTING_FILE.getNewFileType(), sizeType.NEGATIVE_SIZE.getSizeType(), expectedOutputType.NO_CONTENT_COPIED, expectExceptionType.NO}
        });
    }
    
    @Test
    public void moveToNewLocationTest() throws IOException {
        if (expectException == expectExceptionType.YES) {
            assertThrows(Exception.class, () -> fileInfo.moveToNewLocation(newFile, size));
        } else {
            fileInfo.moveToNewLocation(newFile, size);
            
            assertFalse(currentFile.exists());
            assertTrue(newFile.exists());
            
            NEW_FILE_SIZE = newFile.length();
            
            switch(expectedOutput) {
                case NO_CONTENT_COPIED:
                    assertEquals(NEW_FILE_SIZE, newLocationFile.length());
                    break;
                case PARTIAL_CONTENT_COPIED:
                    assertNotEquals(CURRENT_FILE_SIZE, NEW_FILE_SIZE);
                    break;
                case ALL_CONTENT_COPIED:
                    assertEquals(CURRENT_FILE_SIZE, NEW_FILE_SIZE);
                    break;
            }
        }
    }
}