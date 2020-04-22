package structures;

import org.junit.*;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;

public class BlockSortedFileTest {

    private static final String TEST_FILE_NAME = "test_block_file";

    private IBlockSortedFile<String, TestItem> file;

    /**
     * Log of file events during test. Each file event has own key in this map. Value is list of all event of this type.
     * Example:
     * {
     *     BLOCK_READ: [50, 25, 13, 7, 9, 8],
     *     BLOCK_WRITTEN: [12, 53]
     * }
     */
    private static Map<BlockFileAction, List<Integer>> log;

    private static int generateUnsignedHashCode(String text) {
        int result = 0;

        for (int i = 0; i < text.length(); i++) {
            result = (result + (text.charAt(i) * (text.length() - i) * 26)) % Integer.MAX_VALUE;
        }

        return result;
    }

    private void createFile() {
        file = new BlockSortedFile<>(
                TEST_FILE_NAME,
                item -> item.getId(),
                String::hashCode,
                (action, value) -> { // Log event to intLog or stringLog depends on type of event value.
                if (value instanceof Integer) {
                        if (!log.containsKey(action)) {
                            log.put(action, new ArrayList<>());
                        }

                        log.get(action).add((int) value);
                    }
                }
        );
    }

    private TestItem[] createFileAndBuild(int itemsCount) {
        createFile();
        TestItem[] items = getItems(itemsCount);
        file.build(items);
        return items;
    }

    private TestItem[] getItems(int itemsCount) {
        TestItem[] items = new TestItem[itemsCount];

        for (int i = 0; i < itemsCount; i++) {
            items[i] = new TestItem("Item " + String.format("%010d", i) + i, i * i);
        }

        return items;
    }

    @Before
    public void initializeLog() {
        log = new HashMap<>();
    }

    @After
    @Before
    public void removeFile() {
        File file = new File(TEST_FILE_NAME);

        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void createEmptyFile() {
        assertFalse(new File(TEST_FILE_NAME).exists());
        createFile();
        assertTrue(new File(TEST_FILE_NAME).exists());
        assertFalse(log.containsKey(BlockFileAction.BLOCK_WRITTEN)); // No block should be written or read.
        assertFalse(log.containsKey(BlockFileAction.BLOCK_READ));

    }

    @Test
    public void buildEmptyFile() {
        assertFalse(new File(TEST_FILE_NAME).exists());
        createFileAndBuild(0);
        assertTrue(new File(TEST_FILE_NAME).exists());
        assertFalse(log.containsKey(BlockFileAction.BLOCK_WRITTEN)); // No block should be written or read.
        assertFalse(log.containsKey(BlockFileAction.BLOCK_READ));
    }

    @Test
    public void buildSmallFile() {
        assertFalse(new File(TEST_FILE_NAME).exists());
        createFileAndBuild(13);
        assertTrue(new File(TEST_FILE_NAME).exists());
        assertEquals(1, log.get(BlockFileAction.BLOCK_WRITTEN).size()); // Only block 0 should be written.
        assertEquals(0, (int) log.get(BlockFileAction.BLOCK_WRITTEN).get(0));
        assertFalse(log.containsKey(BlockFileAction.BLOCK_READ)); // No block should be read.
    }

    @Test
    public void buildMediumFile() {
        assertFalse(new File(TEST_FILE_NAME).exists());
        createFileAndBuild(100);
        assertTrue(new File(TEST_FILE_NAME).exists());
        assertEquals(1, log.get(BlockFileAction.BLOCK_WRITTEN).size()); // Only block 0 should be written.
        assertEquals(0, (int) log.get(BlockFileAction.BLOCK_WRITTEN).get(0));
        assertFalse(log.containsKey(BlockFileAction.BLOCK_READ)); // No block should be read.
    }

    @Test
    public void buildLargeFile() {
        assertFalse(new File(TEST_FILE_NAME).exists());
        createFileAndBuild(12163);
        assertTrue(new File(TEST_FILE_NAME).exists());
        assertEquals(122, log.get(BlockFileAction.BLOCK_WRITTEN).size()); // 122 blocks should be written.
        assertFalse(log.containsKey(BlockFileAction.BLOCK_READ)); // No block should be read.
    }

    @Test
    public void interpolatingFindItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[4].getId());
        assertEquals(items[4], item); // Searched record should be same as expected record.
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 0 }))); // Only block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindFirstItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[0].getId());
        assertEquals(items[0], item); // Searched record should be same as expected record.
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 0 }))); // Only block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindLastItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[9].getId());
        assertEquals(items[9], item); // Searched record should be same as expected record.
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 0 }))); // Only block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindNonExistingItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating("non-existing-id");
        assertNull(item);
        assertFalse(log.containsKey(BlockFileAction.BLOCK_READ)); // No block should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[99].getId());
        assertEquals(items[99], item); // Searched record should be same as expected record.
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 0 }))); // Only block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindFirstItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[0].getId());
        assertEquals(items[0], item); // Searched record should be same as expected record.
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 0 }))); // Only block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindLastItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[9999].getId());
        assertEquals(items[9999], item); // Searched record should be same as expected record.
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 99 }))); // Only block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindNonExistingItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating("non-existing-id");
        assertNull(item);
        assertTrue(log.get(BlockFileAction.BLOCK_READ).size() <= 4); // Block with min key, block with max key and optionally max 3. another blocks should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindWithOddItemInLargeFile() {
        TestItem[] items = getItems(10001);
        items[10000] = new TestItem("ZZZZZZ-VERY-LONG-id-that-is-different-from-all_other-ids-AAAAAA", 123475);
        createFile();
        file.build(items);

        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[10000].getId());
        assertEquals(items[10000], item);
        assertTrue(log.get(BlockFileAction.BLOCK_READ).size() <= 4); // Block with min key, block with max key and optionally block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindWithOddItemInLargeFile2() {
        TestItem[] items = getItems(10001);
        items[10000] = new TestItem("ZZZZZZ-VERY-LONG-id-that-is-different-from-all_other-ids-AAAAAA", 123475);
        createFile();
        file.build(items);

        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[9999].getId());
        assertEquals(items[9999], item);
        assertTrue(log.get(BlockFileAction.BLOCK_READ).size() <= 4); // Block with min key, block with max key and optionally block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindWithOddItemInLargeFile3() {
        TestItem[] items = getItems(10001);
        items[10000] = new TestItem("ZZZZZZ-VERY-LONG-id-that-is-different-from-all_other-ids-AAAAAA", 123475);
        createFile();
        file.build(items);

        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[25].getId());
        assertEquals(items[25], item);
        assertTrue(log.get(BlockFileAction.BLOCK_READ).size() <= 4); // Block with min key, block with max key and optionally block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindWithOddItemInLargeFile4() {
        TestItem[] items = getItems(10001);
        items[9999] = new TestItem("ZZZZZZ-VERY-LONG-id-that-is-different-from-all_other-ids-AAAAAA", 123475);
        createFile();
        file.build(items);

        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[10000].getId());
        assertEquals(items[10000], item);
        assertTrue(log.get(BlockFileAction.BLOCK_READ).size() <= 4); // Block with min key, block with max key and optionally block with searched record should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindItemInVeryLargeFile() {
        TestItem[] items = createFileAndBuild(50000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[3500].getId());
        assertEquals(items[3500], item); // Searched record should be same as expected record.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void interpolatingFindNonExistingItemInVeryLargeFile() {
        TestItem[] items = createFileAndBuild(50000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating("non-existing-record-id");
        assertNull(item);
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary(items[4].getId());
        assertEquals(items[4], item); // Searched record should be same as expected record.
        assertEquals(1, log.get(BlockFileAction.BLOCK_READ).size()); // Only first block should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindFirstItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary(items[0].getId());
        assertEquals(items[0], item); // Searched record should be same as expected record.
        assertEquals(1, log.get(BlockFileAction.BLOCK_READ).size()); // Only first block should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindLastItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary(items[9].getId());
        assertEquals(items[9], item); // Searched record should be same as expected record.
        assertEquals(1, log.get(BlockFileAction.BLOCK_READ).size()); // Only first block should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindNonExistingItemInSmallFile() {
        TestItem[] items = createFileAndBuild(10);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary("non-existing-id");
        assertNull(item);
        assertEquals(1, log.get(BlockFileAction.BLOCK_READ).size()); // Only first block should be read.
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary(items[4950].getId());
        assertEquals(items[4950], item); // Searched record should be same as expected record.

        // There are 10000 / 100 = 100 blocks. We want record from middle (50th block has index 49) block, so only one block should be read.
        assertEquals(1, log.get(BlockFileAction.BLOCK_READ).size());
        assertEquals(49, (int) log.get(BlockFileAction.BLOCK_READ).get(0));
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindFirstItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary(items[0].getId());
        assertEquals(items[0], item); // Searched record should be same as expected record.

        // There are 10000 / 100 = 100 blocks. We want record from first block, so blocks 49, 24, 11, 5, 2, 0 should be read.
        assertEquals(6, log.get(BlockFileAction.BLOCK_READ).size());
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 49, 24, 11, 5, 2, 0 })));
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindLastItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary(items[9999].getId());
        assertEquals(items[9999], item); // Searched record should be same as expected record.

        // There are 10000 / 100 = 100 blocks. We want record from last block, so blocks 49, 74, 87, 93, 98, 99 should be read.
        assertEquals(7, log.get(BlockFileAction.BLOCK_READ).size());
        assertTrue(log.get(BlockFileAction.BLOCK_READ).equals(Arrays.asList(new Integer[] { 49, 74, 87, 93, 96, 98, 99 })));
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void binaryFindNonExistingItemInLargeFile() {
        TestItem[] items = createFileAndBuild(10000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findBinary("non-existing-id");
        assertNull(item);
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void removeItem() {
        TestItem[] items = createFileAndBuild(1000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[251].getId());
        assertEquals(items[251], item); // Searched record should be same as expected record.
        file.remove(items[251].getId());
        item = file.findInterpolating(items[251].getId());
        assertNull(item); // There should not be the item anymore.
        assertEquals(1, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // Only block with removed item should be written.
    }

    @Test
    public void removeFirstItem() {
        TestItem[] items = createFileAndBuild(1000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[0].getId());
        assertEquals(items[0], item); // Searched record should be same as expected record.
        file.remove(items[0].getId());
        item = file.findInterpolating(items[0].getId());
        assertNull(item); // There should not be the item anymore.
        assertEquals(1, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // Only block with removed item should be written.
    }

    @Test
    public void removeLastItem() {
        TestItem[] items = createFileAndBuild(1000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating(items[999].getId());
        assertEquals(items[999], item); // Searched record should be same as expected record.
        file.remove(items[999].getId());
        item = file.findInterpolating(items[999].getId());
        assertNull(item); // There should not be the item anymore.
        assertEquals(1, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // Only block with removed item should be written.
    }

    @Test
    public void removeNonExistingItem() {
        TestItem[] items = createFileAndBuild(1000);
        int blocksWritten = log.get(BlockFileAction.BLOCK_WRITTEN).size();

        TestItem item = file.findInterpolating("non-existing-id");
        assertNull(item);
        file.remove("non-existing-id");
        item = file.findInterpolating("non-existing-id");
        assertNull(item);
        assertEquals(0, log.get(BlockFileAction.BLOCK_WRITTEN).size() - blocksWritten); // No block should be written.
    }

    @Test
    public void removeFromEmptyFile() {
        TestItem[] items = createFileAndBuild(0);

        TestItem item = file.findInterpolating("non-existing-id");
        assertNull(item);
        file.remove("non-existing-id");
        item = file.findInterpolating("non-existing-id");
        assertNull(item);
        assertFalse(log.containsKey(BlockFileAction.BLOCK_WRITTEN)); // No block should be written.
    }

    @Test
    public void findItemAfterRemoveWholeBlock() {
        TestItem[] items = createFileAndBuild(305);

        TestItem item = file.findInterpolating(items[199].getId());
        assertEquals(items[199], item);

        item = file.findInterpolating(items[300].getId());
        assertEquals(items[300], item);

        for (int i = 199; i < 301; i++) {
            file.remove(items[i].getId());
        }

        item = file.findInterpolating(items[199].getId());
        assertNull(item);

        item = file.findInterpolating(items[300].getId());
        assertNull(item);

        item = file.findInterpolating(items[304].getId());
        assertEquals(items[304], item);

        item = file.findInterpolating(items[198].getId());
        assertEquals(items[198], item);
    }

    @Test
    public void findItemAfterRemoveLastWholeBlock() {
        TestItem[] items = createFileAndBuild(305);

        TestItem item = file.findInterpolating(items[304].getId());
        assertEquals(items[304], item);

        item = file.findInterpolating(items[294].getId());
        assertEquals(items[294], item);

        for (int i = 295; i < 305; i++) {
            file.remove(items[i].getId());
        }

        item = file.findInterpolating(items[304].getId());
        assertNull(item);

        item = file.findInterpolating(items[295].getId());
        assertNull(item);

        item = file.findInterpolating(items[293].getId());
        assertEquals(items[293], item);

        item = file.findInterpolating(items[0].getId());
        assertEquals(items[0], item);
    }

    @Test
    public void findItemAfterRemoveFirstWholeBlock() {
        TestItem[] items = createFileAndBuild(305);

        TestItem item = file.findInterpolating(items[0].getId());
        assertEquals(items[0], item);

        item = file.findInterpolating(items[100].getId());
        assertEquals(items[100], item);

        for (int i = 0; i < 101; i++) {
            file.remove(items[i].getId());
        }

        item = file.findInterpolating(items[0].getId());
        assertNull(item);

        item = file.findInterpolating(items[100].getId());
        assertNull(item);

        item = file.findInterpolating(items[101].getId());
        assertEquals(items[101], item);

        item = file.findInterpolating(items[304].getId());
        assertEquals(items[304], item);
    }

    @Test
    public void findItemAfterRemoveAllRecords() {
        TestItem[] items = createFileAndBuild(205);

        TestItem item = file.findInterpolating(items[0].getId());
        assertEquals(items[0], item);

        item = file.findInterpolating(items[100].getId());
        assertEquals(items[100], item);

        for (int i = 0; i < 205; i++) {
            file.remove(items[i].getId());
        }

        item = file.findInterpolating(items[0].getId());
        assertNull(item);

        item = file.findInterpolating(items[100].getId());
        assertNull(item);
    }

    @Test
    public void findItemAfterRemoveItemInSmallFile() {
        TestItem[] items = { new TestItem("aaa", 1), new TestItem("aab", 2), new TestItem("aac", 3) };
        createFile();
        file.build(items);

        file.findInterpolating(items[0].getId());
    }

}

class TestItem implements Serializable {

    private String id;
    private int value;

    public TestItem(String id, int value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestItem testItem = (TestItem) o;
        return value == testItem.value &&
                id.equals(testItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value);
    }
}