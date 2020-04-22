package structures;

import java.io.Serializable;

public interface IBlockSortedFile<TRecordId, TRecord extends Serializable> {

    void build(TRecord[] records);

    TRecord findInterpolating(TRecordId recordId);

    TRecord findBinary(TRecordId recordId);

    void remove(TRecordId recordId);

}