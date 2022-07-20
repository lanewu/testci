package py.io.sequential;

import java.util.List;

public interface RandomSequentialIdentifierV2 {

  void judgeIOIsSequential(List<? extends IOSequentialTypeHolder> ioContextList);
}
