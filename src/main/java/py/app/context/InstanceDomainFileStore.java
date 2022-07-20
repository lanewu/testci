package py.app.context;

import py.instance.InstanceDomain;



public interface InstanceDomainFileStore {
    public void persistInstanceDomain(InstanceDomain instanceDomain);

    public InstanceDomain getInstanceDomain();

    public static class NullInstanceDomainFileStore implements InstanceDomainFileStore {

        @Override
        public void persistInstanceDomain(InstanceDomain instanceDomain) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public InstanceDomain getInstanceDomain() {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
