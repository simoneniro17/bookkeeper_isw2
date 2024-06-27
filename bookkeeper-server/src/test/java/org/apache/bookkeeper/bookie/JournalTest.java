package org.apache.bookkeeper.bookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.*;

import static org.apache.commons.lang.ArrayUtils.reverse;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(value = Parameterized.class)
public class JournalTest {
    
    private File journalDir;
    private Journal.JournalIdFilter journalIdFilter;
    private List<Long> expectedOutput;
    private expectExceptionType expectException;
    
    public JournalTest(File journalDir, Journal.JournalIdFilter journalIdFilter, List<Long> expectedOutput, expectExceptionType expectException) {
        this.journalDir = journalDir;
        this.journalIdFilter = journalIdFilter;
        this.expectedOutput = expectedOutput;
        this.expectException = expectException;
    }
    
    private enum JournalDirType {
        NOT_EXISTING_DIR,
        EMPTY_DIR,
        ONE_LOG_FILE_DIR,
        NO_LOG_FILES_DIR,
        TWO_LOG_FILES_DIR;
        
        private final String USER_DIR = System.getProperty("user.dir");
        
        public File getJournalDir() {
            switch (this) {
                case NOT_EXISTING_DIR:
                    return new File(USER_DIR, "src/test/resources/journal/not_existing_dir");
                case EMPTY_DIR:
                    return new File(USER_DIR, "src/test/resources/journal/empty_dir");
                case ONE_LOG_FILE_DIR:
                    return new File(USER_DIR, "src/test/resources/journal/one_log_file_dir");
                case NO_LOG_FILES_DIR:
                    return new File(USER_DIR, "src/test/resources/journal/no_log_files_dir");
                case TWO_LOG_FILES_DIR:
                    return new File(USER_DIR, "src/test/resources/journal/two_log_files_dir");
                default:
                    return null;
            }
        }
    }
    
    private enum JournalIdFilterType {
        JOURNAL_ROLLING_FILTER,
        PIPPO_FILTER;
        
        public Journal.JournalIdFilter getJournalIdFilter() {
            switch (this) {
                case JOURNAL_ROLLING_FILTER:
                    return new Journal.JournalIdFilter() {
                        @Override
                        public boolean accept(long journalId) {
                            return journalId < 100;
                        }
                    };
                case PIPPO_FILTER:
                    return new Journal.JournalIdFilter() {
                        @Override
                        public boolean accept(long journalId) {
                            return journalId > 30;
                        }
                    };
                default:
                    return null;
            }
        }
    }
    
    private enum expectExceptionType {
        YES,
        NO
    }
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        if(journalDir != null && Objects.equals(journalDir.getPath(), JournalDirType.TWO_LOG_FILES_DIR.getJournalDir().getPath())) {
            File[] files = journalDir.listFiles();
            
            if(files != null) {
                reverse(files);
                journalDir = Mockito.spy(journalDir);
                doReturn(files).when(journalDir).listFiles();
            }
        }
    }
    
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, null, null, expectExceptionType.YES},
                {JournalDirType.NOT_EXISTING_DIR.getJournalDir(), JournalIdFilterType.PIPPO_FILTER.getJournalIdFilter(), new ArrayList<Long>(), expectExceptionType.NO},
                {JournalDirType.EMPTY_DIR.getJournalDir(), null, new ArrayList<Long>(), expectExceptionType.NO},
                {JournalDirType.NO_LOG_FILES_DIR.getJournalDir(), JournalIdFilterType.PIPPO_FILTER.getJournalIdFilter(), new ArrayList<Long>(), expectExceptionType.NO},
                {JournalDirType.ONE_LOG_FILE_DIR.getJournalDir(), JournalIdFilterType.JOURNAL_ROLLING_FILTER.getJournalIdFilter(), new ArrayList<>(Collections.singletonList(7L)), expectExceptionType.NO},
                
                {JournalDirType.ONE_LOG_FILE_DIR.getJournalDir(), null, new ArrayList<>(Collections.singletonList(7L)), expectExceptionType.NO},
                {JournalDirType.ONE_LOG_FILE_DIR.getJournalDir(), JournalIdFilterType.PIPPO_FILTER.getJournalIdFilter(), new ArrayList<Long>(), expectExceptionType.NO},
                {JournalDirType.TWO_LOG_FILES_DIR.getJournalDir(), null, new ArrayList<>(Arrays.asList(1L, 2L)), expectExceptionType.NO}
        });
    }
    
    @Test
    public void listJournalIdsTest() {
        try {
            List<Long> journalIds = Journal.listJournalIds(journalDir, journalIdFilter);
            assertEquals(journalIds, expectedOutput);
        } catch (Exception e) {
            if (expectException == expectExceptionType.YES)
                assertTrue(true);
            else
                fail();
        }
    }
}