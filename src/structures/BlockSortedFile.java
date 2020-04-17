package structures;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

public class BlockSortedFile<TRecordId, TRecord extends Serializable> implements IBlockSortedFile<TRecordId, TRecord>, Serializable {

    private String fileName;
    private ControlBlock controlBlock;
    private int controlBlockSize;

    private SerializableFunction<TRecord, TRecordId> idAccessor;
    private SerializableFunction<TRecordId, Integer> valueIdAccessor;
    private SerializableBiConsumer<BlockFileAction, Object> logger;

    byte[] byteBuffer;
    Block buffer;

    // Constructor with logger.
    public BlockSortedFile(String fileName, SerializableFunction<TRecord, TRecordId> idAccessor, SerializableFunction<TRecordId, Integer> valueIdAccessor, SerializableBiConsumer<BlockFileAction, Object> logger) {
        this.fileName = fileName;
        this.idAccessor = idAccessor;
        this.valueIdAccessor = valueIdAccessor;
        this.logger = logger;
        createIfNotExists();
    }

    // Constructor without logger.
    public BlockSortedFile(String fileName, SerializableFunction<TRecord, TRecordId> idAccessor, SerializableFunction<TRecordId, Integer> valueIdAccessor) {
        this(fileName, idAccessor, valueIdAccessor, (action, value) -> {});
    }

    /**
     * Create empty block file if there is no file.
     */
    private void createIfNotExists() {
        if (!new File(fileName).exists()) {
            try (RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
                logger.accept(BlockFileAction.FILE_CREATED, fileName);
                buildControlFile(file, 0, 0);
                logger.accept(BlockFileAction.CONTROL_BLOCK_WRITTEN, "blocks: " + controlBlock.blocksCount + ", block factor: " + controlBlock.blockFactor + ", block size: " + (Math.floor(10 * byteBuffer.length / 1024) / 10) + " kB");
            } catch (Exception e) {
                logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
            }
        } else {
            logger.accept(BlockFileAction.FILE_OPENED, fileName);
        }
    }

    /**
     * Create empty block file that contains only control file.
     */
    private void buildControlFile(RandomAccessFile file, int blocksCount, int blockFactor) throws IOException {
        file.setLength(0);
        controlBlock = new ControlBlock(blocksCount, blockFactor, 0);
        byte[] controlBlockBytes = toBytes(controlBlock);
        controlBlockSize = controlBlockBytes.length;

        file.write(ByteBuffer.allocate(Integer.BYTES).putInt(controlBlockBytes.length).array());
        file.write(controlBlockBytes);
    }

    /**
     * Build file that contains (from beginning of file):
     * - Size of control block (Integer),
     * - Control block (ControlBlock),
     * - Data blocks (TRecord).
     * @param records
     */
    @Override
    public void build(TRecord[] records) {
        Arrays.sort(records, Comparator.comparing(r -> valueIdAccessor.apply(idAccessor.apply(r))));

        try (RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
            logger.accept(BlockFileAction.FILE_CREATED, fileName);
            int blockFactor = 100; // TODO: Better estimate of block factor?
            int blocksCount = (int) Math.ceil((double) records.length / blockFactor);
            buildControlFile(file, blocksCount, blockFactor);

            for (int i = 0; i < records.length; i += blockFactor) { // Write all data blocks to file.
                TRecord[] blockRecords = Arrays.copyOfRange(records, i, i + blockFactor);
                Block block = new Block(blockRecords);
                byte[] blockBytes = toBytes(block);

                if (i == 0) {
                    controlBlock.blockSize = blockBytes.length;
                    file.getChannel().position(Integer.BYTES);
                    file.write(toBytes(controlBlock));
                    logControlBlock(BlockFileAction.CONTROL_BLOCK_WRITTEN);
                    goToBlock(file, 0);
                }

                logger.accept(BlockFileAction.BLOCK_WRITTEN, i / blockFactor);
                file.write(blockBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
        }
    }

    private void logControlBlock(BlockFileAction action) {
        logger.accept(action, "blocks: " + controlBlock.blocksCount + ", block factor: " + controlBlock.blockFactor + ", block size: " + (Math.floor(10 * controlBlock.blockSize / 1024) / 10) + " kB");
    }

    @Override
    public TRecord findInterpolating(TRecordId recordId) {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
            int recordIndex = findRecordInterpolating(recordId, file);
            return recordIndex == -1 ? null : buffer.records[recordIndex];
        } catch (Exception e) {
            e.printStackTrace();
            logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
        }

        return null;
    }

    @Override
    public TRecord findBinary(TRecordId recordId) {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
            logger.accept(BlockFileAction.SEARCH_START, recordId);

            readControlBlock(file);

            if (controlBlock.blocksCount == 0) {
                logger.accept(BlockFileAction.RECORD_NOT_FOUND, recordId);
                return null;
            }

            int start = 0;
            int end = controlBlock.blocksCount - 1;

            while (start <= end) {
                int blockIndex = (end + start) / 2;
                logger.accept(BlockFileAction.SEARCH_INTERVAL, start + "-" + end);
                readBlock(file, blockIndex);

                int recordIndex = buffer.indexOfRecord(recordId);

                if (recordIndex == -1) { // Record was not found in current block.
                    int idValue = valueIdAccessor.apply(recordId);
                    int firstIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.getFirstRecord())); // TODO: Fix when record is empty.
                    int lastIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.getLastRecord())); // TODO: Fix when record is empty.

                    if (idValue < firstIdValue) { // If search ID is smaller then smallest ID in current block, search for previous block.
                        end = blockIndex - 1;
                    } else if (idValue > lastIdValue) { // If search ID is greater than greatest ID in current block, search for next block.
                        start = blockIndex + 1;
                    }
                } else {
                    logger.accept(BlockFileAction.RECORD_FOUND, buffer.records[recordIndex]);
                    return buffer.records[recordIndex];
                }
            }
        } catch (Exception e) {
            logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
        }

        logger.accept(BlockFileAction.RECORD_NOT_FOUND, recordId);
        return null;
    }

    /**
     * Find record by ID using algorithm bellow (interpolating search):
     * - Estimate record position in file depends on min key in file, max key in file and key of record,
     * - Check if searched record is in estimated block. If so, return record and exit.
     * - If not and:
     *    - key of searched record is lower than lowest key in block, read previous block and repeat algorithm,
     *    - key of searched record is higher than highest key in block, read next block and repeat algorithm,
     *    - else record was not found.
     * @return Index of record in current block (current block is in buffer). -1 if record was not found.
     */
    private int findRecordInterpolating(TRecordId recordId, RandomAccessFile file) throws IOException, ClassNotFoundException {
        logger.accept(BlockFileAction.SEARCH_START, recordId);
        readControlBlock(file);

        if (controlBlock.blocksCount == 0) {
            logger.accept(BlockFileAction.RECORD_NOT_FOUND, recordId);
            return -1;
        }

        int blockIndex = estimateBlockIndex(file, recordId);

        while (blockIndex >= 0 && blockIndex < controlBlock.blocksCount) {
            readBlock(file, blockIndex);
            int recordIndex = buffer.indexOfRecord(recordId);

            if (recordIndex == -1) { // Record was not found in current block.
                int idValue = valueIdAccessor.apply(recordId);
                int firstIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.getFirstRecord())); // TODO: Fix when record is empty.
                int lastIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.getLastRecord())); // TODO: Fix when record is empty.

                if (idValue < firstIdValue) { // If search ID is smaller then smallest ID in current block, search for previous block.
                    logger.accept(BlockFileAction.SEARCH_ANOTHER_BLOCK, "previous");
                    blockIndex--;
                } else if (idValue > lastIdValue) { // If search ID is greater than greatest ID in current block, search for next block.
                    logger.accept(BlockFileAction.SEARCH_ANOTHER_BLOCK, "next");
                    blockIndex++;
                } else { // Else record was not found.
                    logger.accept(BlockFileAction.RECORD_NOT_FOUND, recordId);
                    return -1;
                }
            } else {
                logger.accept(BlockFileAction.RECORD_FOUND, buffer.records[recordIndex]);
                return recordIndex;
            }
        }

        logger.accept(BlockFileAction.RECORD_NOT_FOUND, recordId);
        return -1;
    }

    @Override
    public void remove(TRecordId recordId) {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
            logger.accept(BlockFileAction.REMOVE_START, recordId);
            int recordIndex = findRecordInterpolating(recordId, file);

            if (recordIndex != -1) {
                logger.accept(BlockFileAction.RECORD_REMOVED, buffer.records[recordIndex]);
                buffer.records[recordIndex] = null;

                file.write(toBytes(buffer));
            }
        } catch (Exception e) {
            logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
        }
    }

    /**
     * Calculate relative distance of record from beginning of file: d = (K - Kmin) / (Kmax - Kmin).
     * Then estimate block index from relative distance from beginning of file.
     */
    private int estimateBlockIndex(RandomAccessFile file, TRecordId recordId) throws IOException, ClassNotFoundException {
        int idValue = valueIdAccessor.apply(recordId);

        readBlock(file, 0); // Read key of first record in first block.
        int firstIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.getFirstRecord())); // TODO: Record could be null.

        readBlock(file, controlBlock.blocksCount - 1); // REad ky of last record in last block.
        int lastIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.getLastRecord())); // TODO: Record could be null.

        double relativeDistance = ((double) idValue - firstIdValue) / (lastIdValue - firstIdValue);
        logger.accept(BlockFileAction.RELATIVE_DISTANCE_CALCULATED, relativeDistance);

        return Math.min((int) Math.floor(controlBlock.blocksCount * relativeDistance), controlBlock.blocksCount - 1);
    }

    /**
     * Set position in file to nth data block.
     */
    private void goToBlock(RandomAccessFile file, int nth) throws IOException {
        int position = nth * controlBlock.blockSize + controlBlockSize + Integer.BYTES;
        file.getChannel().position(position);
    }

    /**
     * Read control block from file.
     */
    private void readControlBlock(RandomAccessFile file) throws IOException, ClassNotFoundException {
        file.getChannel().position(0);
        controlBlockSize = file.readInt();
        byte[] controlBlockBuffer = new byte[controlBlockSize];
        file.read(controlBlockBuffer);
        controlBlock = (ControlBlock) fromBytes(controlBlockBuffer);
        byteBuffer = new byte[controlBlock.blockSize];
        logControlBlock(BlockFileAction.CONTROL_BLOCK_READ);
    }

    /**
     * Move nth data block from file to buffer.
     */
    private void readBlock(RandomAccessFile file, int nth) throws IOException, ClassNotFoundException {
        goToBlock(file, nth);
        file.read(byteBuffer);
        buffer = (Block) fromBytes(byteBuffer);
        logger.accept(BlockFileAction.BLOCK_READ, nth);
    }

    /**
     * Convert object to byte array.
     */
    private static byte[] toBytes(Object object) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream oOut = new ObjectOutputStream(bOut);
        oOut.writeObject(object);
        return bOut.toByteArray();
    }

    /**
     * Convert byte array to object.
     */
    private static Object fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bIn = new ByteArrayInputStream(data);
        ObjectInputStream oIn = new ObjectInputStream(bIn);
        return oIn.readObject();
    }

    private class ControlBlock implements Serializable {

        int blocksCount;
        int blockFactor;
        int blockSize;

        public ControlBlock(int blocksCount, int blockFactor, int blockSize) {
            this.blocksCount = blocksCount;
            this.blockFactor = blockFactor;
            this.blockSize = blockSize;
        }
    }

    private class Block implements Serializable {

        TRecord[] records;

        public Block(TRecord[] records) {
            this.records = records;
        }

        /**
         * Get index of record in block.
         */
        private int indexOfRecord(TRecordId recordId) {
            for (int i = 0; i < records.length; i++) { // TODO: Binary search?
                if (records[i] != null && idAccessor.apply(records[i]).equals(recordId)) {
                    return i;
                }
            }

            return -1;
        }

        /**
         * Get first non-null record from block.
         */
        private TRecord getFirstRecord() {
            for (int i = 0; i < records.length; i++) {
                if (records[i] != null) {
                    return records[i];
                }
            }

            return null;
        }

        /**
         * Get last non-null record from block.
         */
        private TRecord getLastRecord() {
            for (int i = records.length - 1; i >= 0; i--) {
                if (records[i] != null) {
                    return records[i];
                }
            }

            return null;
        }

    }

}
