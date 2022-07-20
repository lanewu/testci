package py.common.file;

import java.util.List;

/**
 * an interface for recording a str in a file
 */
public interface FileRecorder {
    String ENCODING = "utf-8";

    boolean add(String record);

    boolean remove(String record);

    boolean fileExist();

    boolean contains(String record);

    boolean isEmpty();

    List<String> records();
}
