package py.app.context;

import py.instance.Group;

public class DummyGroupStore implements GroupStore {
    private Group group;
    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public void persistGroup(Group group) throws Exception {
        this.group = group;
    }

    @Override
    public boolean canUpdateGroup(Group newGroup) throws Exception {
        return false;
    }

}
