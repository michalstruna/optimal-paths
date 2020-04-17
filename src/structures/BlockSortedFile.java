package structures;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

public class BlockSortedFile<TRecordId, TRecord extends Serializable> implements IObjectFile<TRecordId, TRecord>, Serializable {

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
        logger.accept(BlockFileAction.FILE_OPENED, fileName);
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
                buildControlFile(file, 0, 0);
                logger.accept(BlockFileAction.FILE_CREATED, fileName);
            } catch (Exception e) {
                logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
            }
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
                    goToBlock(file, 0);
                }

                file.write(blockBytes);
            }

            logger.accept(BlockFileAction.FILE_CREATED, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
        }
    }

    @Override
    public TRecord findInterpolating(TRecordId recordId) {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
            int recordIndex = findRecordPositionInterpolating(recordId, file);
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

                System.out.println(start + " " + blockIndex + " " + end);

                readBlock(file, blockIndex);
                logger.accept(BlockFileAction.SEARCH_INTERVAL, start + "-" + end);
                logger.accept(BlockFileAction.READ_BLOCK, blockIndex);

                int recordIndex = buffer.indexOfRecord(recordId);

                if (recordIndex == -1) { // Record was not found in current block.
                    int idValue = valueIdAccessor.apply(recordId);
                    int firstIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.records[0])); // TODO: Fix when record is empty.
                    int lastIdValue = valueIdAccessor.apply(idAccessor.apply(getLastRecord(buffer))); // TODO: Fix when record is empty.

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
     * Find record.
     * @param recordId ID of searched record.
     * @param file
     * @return Pair where key is index of block in which record is and value is index of record in block.
     */
    private int findRecordPositionInterpolating(TRecordId recordId, RandomAccessFile file) throws IOException, ClassNotFoundException { // TODO: Pair is not needed, only record index. Block is in buffer.
        logger.accept(BlockFileAction.SEARCH_START, recordId);

        readControlBlock(file);

        if (controlBlock.blocksCount == 0) {
            logger.accept(BlockFileAction.RECORD_NOT_FOUND, recordId);
            return -1;
        }

        double distance = calculateRelativeDistance(file, recordId);
        int blockIndex = estimateBlockIndex(distance);
        logger.accept(BlockFileAction.RELATIVE_DISTANCE_CALCULATED, distance);

        while (true) {
            if (blockIndex < 0 || blockIndex >= controlBlock.blocksCount) {
                logger.accept(BlockFileAction.RECORD_NOT_FOUND, recordId);
                return -1;
            }

            readBlock(file, blockIndex);
            logger.accept(BlockFileAction.READ_BLOCK, blockIndex);

            int recordIndex = buffer.indexOfRecord(recordId);

            if (recordIndex == -1) { // Record was not found in current block.
                int idValue = valueIdAccessor.apply(recordId);
                int firstIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.records[0])); // TODO: Fix when record is empty.
                int lastIdValue = valueIdAccessor.apply(idAccessor.apply(getLastRecord(buffer))); // TODO: Fix when record is empty.

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
    }

    /**
     * Calculate relative distance of record from beginning of file.
     * d = (K - Kmin) / (Kmax - Kmin)
     */
    private double calculateRelativeDistance(RandomAccessFile file, TRecordId recordId) throws IOException, ClassNotFoundException {
        int idValue = valueIdAccessor.apply(recordId);

        readBlock(file, 0);
        int firstIdValue = valueIdAccessor.apply(idAccessor.apply(buffer.records[0])); // TODO: Fix when record is empty.

        readBlock(file, controlBlock.blocksCount - 1);
        int lastIdValue = valueIdAccessor.apply(idAccessor.apply(getLastRecord(buffer))); // TODO: Fix when record is empty.

        return ((double) idValue - firstIdValue) / (lastIdValue - firstIdValue);
    }

    /**
     * Estimate block index from relative distance from beginning of file.
     */
    private int estimateBlockIndex(double relativeDistance) {
        return (int) Math.floor(controlBlock.blocksCount * relativeDistance);
    }

    @Override
    public void remove(TRecordId recordId) {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
            logger.accept(BlockFileAction.REMOVE_START, recordId);
            int recordIndex = findRecordPositionInterpolating(recordId, file);

            if (recordIndex != -1) {
                logger.accept(BlockFileAction.RECORD_REMOVED, buffer.records[recordIndex]);
                buffer.records[recordIndex] = null;

                file.write(toBytes(buffer));
            }
        } catch (Exception e) {
            logger.accept(BlockFileAction.EXCEPTION, e.getMessage());
        }
    }

    private TRecord getLastRecord(Block block) {
        for (int i = block.records.length - 1; i >= 0; i--) {
            if (block.records[i] != null) {
                return block.records[i];
            }
        }

        return null;
    }

    /**
     * Set position in file to nth data block.
     * @param file
     * @param nth
     * @throws IOException
     */
    private void goToBlock(RandomAccessFile file, int nth) throws IOException {
        int position = nth * controlBlock.blockSize + controlBlockSize + Integer.BYTES;
        file.getChannel().position(position);
    }

    /**
     * Read control block from file.
     * @param file
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readControlBlock(RandomAccessFile file) throws IOException, ClassNotFoundException {
        file.getChannel().position(0);
        controlBlockSize = file.readInt();
        byte[] controlBlockBuffer = new byte[controlBlockSize];
        file.read(controlBlockBuffer);
        controlBlock = (ControlBlock) fromBytes(controlBlockBuffer);
        byteBuffer = new byte[controlBlock.blockSize];
        logger.accept(BlockFileAction.CONTROL_BLOCK_READ, "blocks: " + controlBlock.blocksCount + ", block factor: " + controlBlock.blockFactor);
    }

    /**
     * Move nth data block from file to buffer.
     * @param file Opened file.
     * @param nth Order of data block (0 = first).
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readBlock(RandomAccessFile file, int nth) throws IOException, ClassNotFoundException {
        goToBlock(file, nth);
        file.read(byteBuffer);
        buffer = (Block) fromBytes(byteBuffer);
    }

    /**
     * Convert object to byte array.
     * @param object Any object.
     * @return Serialized object.
     * @throws IOException
     */
    private static byte[] toBytes(Object object) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream oOut = new ObjectOutputStream(bOut);
        oOut.writeObject(object);
        return bOut.toByteArray();
    }

    /**
     * Convert byte array to object.
     * @param data Byte array.
     * @return Deserialized object.
     * @throws IOException
     * @throws ClassNotFoundException
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
         * @param recordId
         * @return Index of record. If record was not found returns -1.
         */
        private int indexOfRecord(TRecordId recordId) {
            for (int i = 0; i < records.length; i++) {
                if (records[i] != null && idAccessor.apply(records[i]).equals(recordId)) {
                    return i;
                }
            }

            return -1;
        }

    }

}
