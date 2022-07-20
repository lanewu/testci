package py.common.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IdUtils {

    public static List<Long> toLongs(Collection<? extends AbstractId> ids) {
        List<Long> longIds = new ArrayList<Long>();
        if (ids != null) {
            for (AbstractId id : ids) {
                longIds.add(id.getId());
            }
        }
        return longIds;
    }
}
