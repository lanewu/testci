package py.zookeeper;

import java.util.List;

/**
 * It supplies operators for zookeeper
 * 
 * @author lx
 *
 */
public interface ZkClient {

    /**
     * get all files in the path
     * 
     * @param path
     * @return
     * @throws ZkException
     */
    public List<String> getFiles(String path) throws ZkException;

    public List<String> getFiles(String path, boolean watch) throws ZkException;

    /**
     * registry a listener with the specified path
     * 
     * @param path
     */
    public void monitor(String path) throws ZkException;

    /**
     * read from the file
     * 
     * @param path
     * @return
     * @throws ZkException
     */
    public byte[] readData(String path) throws ZkException;

    /**
     * write to the file
     * 
     * @param path
     * @param data
     * @throws ZkException
     */
    public void writeData(String path, byte[] data) throws ZkException;

    /**
     * create the path with parent directory
     * 
     * @param path
     * @throws Exception
     */
    public void createPath(String path) throws ZkException;

    /**
     * create a file and write some data if there is data
     * 
     * @param path
     * @param data
     * @param ephemeral
     * @return
     * @throws ZkException
     */
    public String createFile(String path, byte[] data, boolean ephemeral) throws ZkException;

    /**
     * recycle the resource
     */
    public void close();
    
    /**
     * delete a file in zookeeper
     * 
     * @param path
     */
    public void deleteFile(String path) throws ZkException;
    
    /**
     * 
     * @param path
     * @return
     * @throws ZkException
     */
    public boolean exist(String path) throws ZkException;
}
