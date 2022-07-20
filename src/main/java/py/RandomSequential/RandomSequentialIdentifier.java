package py.RandomSequential;

import py.common.struct.Pair;

import java.util.List;

public interface RandomSequentialIdentifier {

    boolean updateLastOffsetAndIsSequential(long offset, int length);

}
