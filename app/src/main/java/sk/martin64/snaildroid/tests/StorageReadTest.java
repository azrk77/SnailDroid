package sk.martin64.snaildroid.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.channels.FileChannel;

import sk.martin64.snaildroid.view.MeasureGraphView;
import sk.martin64.snaildroid.view.Utils;

public class StorageReadTest implements TestBase {

    private File file;
    private long started = 0;
    private long dlStart;
    private DummyOutputStream baos;
    private DefaultLongGraphAdapterImpl adapter = new DefaultLongGraphAdapterImpl();

    public StorageReadTest(File file) {
        this.file = file;
    }

    @Override
    public int run() {
        started = System.currentTimeMillis();
        baos = new DummyOutputStream();

        try (InputStream is = new MarkableFileInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[2048];
            int read;

            while (!Thread.interrupted()) {
                dlStart = System.nanoTime();
                baos.reset();
                while ((read = is.read(buffer)) > 0) {
                    if (Thread.interrupted()) { // caller wants test to stop
                        return CODE_OK;
                    }
                    baos.write(buffer, 0, read);
                }
                is.reset();
            }
        } catch (InterruptedIOException e) {
            e.printStackTrace();
            return CODE_OK;
        } catch (IOException e) {
            e.printStackTrace();
            return CODE_EXCEPTION;
        }

        return CODE_OK;
    }

    @Override
    public String getSpeed(int unit, int x) {
        long s = baos.getLength();
        long est = System.nanoTime() - dlStart;
        double estSec = est / 1000000000d;
        long speed = estSec > 0 ? (long) (s / estSec) : 0;

        adapter.addPoint(x, speed);

        if (unit == UNIT_BYTE)
            return String.format("%s/s", Utils.humanReadableByteCountSI(speed, 2));
        else if (unit == UNIT_BIT)
            return String.format("%s/s", Utils.humanReadableBitsCount(speed, 2));
        else return null;
    }

    @Override
    public MeasureGraphView.GraphAdapter<?> getGraphAdapter() {
        return adapter;
    }

    @Override
    public long getTimeStarted() {
        return started;
    }

    @Override
    public long getDataUsed() {
        return baos.getLengthTotal();
    }

    @Override
    public String getName() {
        return "Storage read speed";
    }

    public static class MarkableFileInputStream extends FilterInputStream {
        private FileChannel myFileChannel;
        private long mark = -1;

        public MarkableFileInputStream(FileInputStream fis) {
            super(fis);
            myFileChannel = fis.getChannel();
            mark(0);
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                mark = myFileChannel.position();
            } catch (IOException ex) {
                mark = -1;
            }
        }

        @Override
        public synchronized void reset() throws IOException {
            if (mark == -1) {
                throw new IOException("not marked");
            }
            myFileChannel.position(mark);
        }
    }
}