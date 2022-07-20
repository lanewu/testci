package py.kvstore.etcd;

public final class JetcdClientBuilder {
    String[] endpoints;
    public JetcdClientBuilder endpoints(String[] endpoints) {
        this.endpoints = endpoints;
        return this;
    }

    public JetcdClient build() {
        return new JetcdClientImpl(endpoints);
    }
}
