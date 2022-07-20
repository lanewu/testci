package py.store;

public interface ICommitter<TPath,TData> {
    public void to(TPath path) throws Exception;

    public ICommitter<TPath,TData> inFormatOf(Class<TData> clazz) throws Exception;
}
