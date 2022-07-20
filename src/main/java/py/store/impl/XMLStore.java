package py.store.impl;

import java.io.File;
import java.io.FileReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.monitor.exception.EmptyException;
import py.monitor.exception.EmptyStoreException;
import py.store.ICommitter;
import py.store.ILoader;

/**
 * 
 * @author shixulu
 *
 * @param <TData>
 */
public class XMLStore<TData> {
    private static final Logger logger = LoggerFactory.getLogger(XMLStore.class);
    private Class<TData> clazz;
    protected TData data;

    public class Loader implements ILoader<String> {

        @Override
        @SuppressWarnings("unchecked")
        public void from(String path) throws EmptyStoreException, Exception {
            logger.debug("File path : {}", path);

            File xmlFile = new File(path);
            if (!xmlFile.exists()) {
                xmlFile.createNewFile();
                throw new EmptyStoreException();
            } else {
                if (xmlFile.getTotalSpace() == 0l) {
                    throw new EmptyStoreException();
                } else {
                    FileReader fileReader = new FileReader(xmlFile);
                    JAXBContext context = JAXBContext.newInstance(clazz);
                    Unmarshaller um = context.createUnmarshaller();
                    data = (TData) um.unmarshal(fileReader);
                }
            }
        }

    }

    public class Committer implements ICommitter<String, TData> {
        private Class<TData> clazz;

        @Override
        public void to(String path) throws Exception {
            logger.debug("Going to write data to disk, file path is : {}", path);

            // create JAXB context and instantiate marshaller
            JAXBContext context = JAXBContext.newInstance(this.clazz);
            Marshaller mmarshaller = context.createMarshaller();
            mmarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            // Write to File
            mmarshaller.marshal(data, new File(path));
        }

        @Override
        public ICommitter<String, TData> inFormatOf(Class<TData> clazz) throws Exception {
            this.clazz = clazz;
            return this;
        }

    }

    public ILoader<String> load(Class<TData> clazz) throws EmptyException, Exception {
        this.clazz = clazz;
        try {
            return new Loader();
        } catch (Exception e) {
            logger.error("Caught an exception", e);
            throw e;
        }
    }

    public ICommitter<String, TData> commit(TData data) throws Exception {
        this.data = data;
        return new Committer();
    }

}
