package structures;

import java.io.Serializable;
import java.util.function.BiConsumer;

public interface SerializableBiConsumer<T, U> extends BiConsumer<T, U>, Serializable {

}