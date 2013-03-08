/*
 * Copyright 2013 Wotif Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.wotifgroup.zopfli;

import com.ochafik.lang.jnaerator.runtime.CharByReference;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.Buffer;
import java.util.zip.Adler32;
import java.util.zip.Deflater;

/**
 * An implementation of {@link Deflater} that uses Zopfli to deflate the provided data. This class can be used with both
 * {@link java.util.zip.DeflaterOutputStream} and {@link java.util.zip.DeflaterInputStream}
 */
public class JopfliDeflater extends Deflater {
    public static int MASTER_BLOCK_SIZE = 20 * 1024;

    private CompositeByteBuf buf;
    private int masterBlockSize;
    private ZopfliLibrary zopfliLibrary;
    private PointerByReference out;
    private NativeSizeByReference outSize;
    private NativeSize currentOutOffset;
    private CharByReference bp;
    private ZopfliLibrary.OptionsStruct optionsStruct;
    private Adler32 adler32;
    private boolean zlibWrapping;       // True if we're outputting ZLIB header + footer. false for just raw DEFLATE.
    private State state;
    private boolean eof;

    private byte headerState;       // 1 == CMF. 2 == FLG
    private byte trailerState;      // 1 - 4

    public JopfliDeflater() {
        this(false);
    }

    public JopfliDeflater(boolean nowrap) {
        this(MASTER_BLOCK_SIZE, nowrap);
    }

    public JopfliDeflater(int masterBlockSize) {
        this(masterBlockSize, false);
    }

    public JopfliDeflater(int masterBlockSize, boolean nowrap) {
        this.zlibWrapping = !nowrap;
        this.masterBlockSize = masterBlockSize;
        this.zopfliLibrary = Jopfli.LIB;
        this.optionsStruct = ZopfliLibrary.OptionsStruct.of(Options.SMALL_FILE_DEFAULTS);   // TODO: configurable.
        this.adler32 = new Adler32();
        this.reset();
    }

    @Override
    public void reset() {
        this.bp = new CharByReference((char)0);
        this.out = new PointerByReference(new Pointer(0));
        this.outSize =  new NativeSizeByReference();
        this.currentOutOffset = new NativeSize(0);
        this.buf = Unpooled.compositeBuffer();
        this.state = this.zlibWrapping ? State.HEADER : State.DEFLATING;
        this.eof = false;
        this.headerState = 1;
        this.trailerState = 1;
        this.adler32.reset();
    }

    @Override
    public void setDictionary(byte[] b, int off, int len) {
        throw new IllegalAccessError();
    }

    @Override
    public void setDictionary(byte[] b) {
        throw new IllegalAccessError();
    }

    @Override
    public void setStrategy(int strategy) {
        throw new IllegalAccessError();
    }

    @Override
    public void setLevel(int level) {
        throw new IllegalAccessError();
    }

    @Override
    public int getAdler() {
        throw new IllegalAccessError();
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        this.buf.addComponent(Unpooled.wrappedBuffer(b, off, len));
        this.buf.writerIndex(this.buf.capacity());

        this.adler32.update(b, off, len);
    }

    @Override
    public void end() {
        // TODO: anything we should be doing here?
    }

    @Override
    public boolean needsInput() {
        return (this.state == State.DEFLATING || this.state == State.HEADER)
            && !this.eof && this.buf.readableBytes() < this.masterBlockSize;
    }

    @Override
    public int deflate(byte[] b, int off, int len) {
        this.doDeflate(b, off, len);

        return -1;
    }

    private int doDeflate(byte[] b, int off, int len) {
        int available = len;

        loop: while(available > 0 && this.state != State.FINISHED) {
            switch(this.state) {
                case HEADER: {
                    while(this.headerState <= 3 && available > 0) {
                        switch(this.headerState++) {
                            case 1:
                                b[off++] = (byte)120;
                                available--;
                                break;
                            case 2:
                                b[off++] = (byte)1;
                                available--;
                                break;
                            case 3:
                                this.state = State.DEFLATING;
                                break;
                        }
                    }
                    break;
                }
                case DEFLATING: {
                    this.runZopfliDeflate();

                    int outputAvailable = this.outSize.getValue().intValue() - this.currentOutOffset.intValue();
                    if(outputAvailable == 0) break loop;

                    int toRead = Math.min(outputAvailable, available);
                    this.out.getValue().read(this.currentOutOffset.intValue(), b, off, toRead);
                    this.currentOutOffset.setValue(this.currentOutOffset.longValue() + toRead);

                    off += toRead;
                    available -= toRead;

                    if(this.eof && !this.buf.isReadable() && this.currentOutOffset.intValue() == this.outSize.getValue().intValue()) {
                        this.state = this.zlibWrapping ? State.TRAILER : State.FINISHED;
                    }

                    break;
                }
                case TRAILER: {
                    long adler32Value = this.adler32.getValue();

                    while(this.trailerState <= 4 && available > 0) {
                        b[off++] = (byte)((adler32Value >> ((4 - this.trailerState++) * 8)) & 0xFF);
                        available--;
                    }

                    if(this.trailerState == 5) {
                        this.state = State.FINISHED;
                    }

                    break;
                }
            }
        }

        return len - available;
    }

    @Override
    public void finish() {
        if(this.state != State.DEFLATING && this.state != State.HEADER) {
            throw new IllegalStateException("Finish called at inappropriate time.");
        }
        this.eof = true;
    }

    @Override
    public boolean finished() {
        return this.state == State.FINISHED;
    }

    /**
     * Flushes data out to actual Zopfli implementation. Should only flush out to Zopfli when buffered data exceeds
     * maximum master block size, or finish() has been called.
     */
    private void runZopfliDeflate() {
        int readableBytes = this.buf.readableBytes();

        if(readableBytes == 0) return;

        Buffer in = this.buf.readBytes(Math.min(this.masterBlockSize, readableBytes)).nioBuffer();
        boolean allDone = this.eof && !this.buf.isReadable();
        NativeSize inSize = new NativeSize(in.remaining());
        this.zopfliLibrary.ZopfliDeflatePart(this.optionsStruct, 2, allDone ? 1 : 0, in, new NativeSize(0),
            inSize, this.bp, this.out, this.outSize);
    }

    private enum State {
        HEADER,             // Outputting zlib header.
        DEFLATING,          // Receiving and deflating data.
        TRAILER,            // Outputting zlib trailer.
        FINISHED            // ... and that's the way the cookie crumbles.
    }
}
