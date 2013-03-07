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
    private boolean finished;

    public JopfliDeflater() {
        this(MASTER_BLOCK_SIZE);
    }

    public JopfliDeflater(int masterBlockSize) {
        this.masterBlockSize = masterBlockSize;
        this.zopfliLibrary = Jopfli.LIB;
        this.optionsStruct = ZopfliLibrary.OptionsStruct.of(Options.SMALL_FILE_DEFAULTS);   // TODO: configurable.
        this.reset();
    }

    @Override
    public void reset() {
        this.bp = new CharByReference((char)0);
        this.out = new PointerByReference(new Pointer(0));
        this.outSize =  new NativeSizeByReference();
        this.currentOutOffset = new NativeSize(0);
        this.buf = Unpooled.compositeBuffer();
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
    }

    @Override
    public int deflate(byte[] b, int off, int len) {
        this.runDeflate();

        int available = this.outSize.getValue().intValue() - this.currentOutOffset.intValue();
        if(available == 0) return 0;

        int read = Math.min(available, len);
        this.out.getValue().read(this.currentOutOffset.intValue(), b, off, read);
        this.currentOutOffset.setValue(this.currentOutOffset.longValue() + read);
        return read;
    }

    @Override
    public void finish() {
        this.finished = true;
        this.runDeflate();
    }

    @Override
    public boolean finished() {
        return this.finished && this.currentOutOffset.intValue() == this.outSize.getValue().intValue();
    }

    private void runDeflate() {
        if(this.buf.readableBytes() < this.masterBlockSize && !this.finished) {
            return;
        }

        Buffer in = this.buf.readBytes(Math.min(this.masterBlockSize, this.buf.readableBytes())).nioBuffer();
        NativeSize inSize = new NativeSize(in.remaining());
        this.zopfliLibrary.ZopfliDeflatePart(this.optionsStruct, 2, this.finished ? 1 : 0, in, new NativeSize(0),
            inSize, this.bp, this.out, this.outSize);
    }
}
