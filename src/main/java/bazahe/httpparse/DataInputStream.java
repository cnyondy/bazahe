package bazahe.httpparse;

import net.dongliu.commons.io.InputOutputs;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream can read data
 *
 * @author Liu Dong
 */
abstract class DataInputStream extends FilterInputStream {

    protected DataInputStream(InputStream in) {
        super(in);
    }

    /**
     * Read exactly size bytes
     */
    public byte[] readExact(int size) throws IOException {
        byte[] buffer = new byte[size];
        int read = InputOutputs.readExact(this, buffer);
        if (read != size) {
            throw new EOFException("Unexpected end of stream");
        }
        return buffer;
    }

    /**
     * Read two byte unsigned value
     */
    public int readUnsigned2() throws IOException {
        return (this.read() << 8) + this.read();
    }

    /**
     * Read three byte unsigned value
     */
    public int readUnsigned3() throws IOException {
        return (this.read() << 16) + (this.read() << 8) + this.read();
    }

    /**
     * Read four byte unsigned value
     */
    public int readUnsigned4() throws IOException {
        return (this.read() << 24) + (this.read() << 16) + (this.read() << 8) + this.read();
    }

    /**
     * Read eight byte unsigned value
     */
    public long readUnsigned8() throws IOException {
        return ((long) this.read() << 56)
                + ((long) this.read() << 48)
                + ((long) this.read() << 40)
                + ((long) this.read() << 32)
                + ((long) this.read() << 24)
                + ((long) this.read() << 16)
                + ((long) this.read() << 8)
                + ((long) this.read());
    }
}
