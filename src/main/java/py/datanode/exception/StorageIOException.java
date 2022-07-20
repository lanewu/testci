package py.datanode.exception;

import py.exception.StorageException;

/*
* if happens a error Or timeout when write/read data from disk, by hard device error. throw this exception.
* then, this storage be set DEGRADED, and the segment unit on this position will be deleted.
*/

public class StorageIOException extends StorageException {

  public StorageIOException(long offset, long length) {
    setOffset(offset);
    setLength(length);
    ioException = true;
  }
}
