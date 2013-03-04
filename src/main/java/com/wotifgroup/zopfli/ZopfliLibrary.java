package com.wotifgroup.zopfli;

import com.ochafik.lang.jnaerator.runtime.CharByReference;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Library;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface ZopfliLibrary extends Library {
    void ZlibCompress(OptionsStruct options, byte[] in, NativeSize insize, PointerByReference out,
            NativeSizeByReference outsize);

    void GzipCompress(OptionsStruct options, byte[] in, NativeSize insize, PointerByReference out,
            NativeSizeByReference outsize);

    void Deflate(OptionsStruct options, int btype, int _final, byte[] in, NativeSize insize, CharByReference bp,
            PointerByReference out, NativeSizeByReference outsize);

    public static final class OptionsStruct extends Structure {
        private static final List<String> FIELD_ORDER = Collections.unmodifiableList(new ArrayList<String>() {{
            this.add("verbose");
            this.add("numiterations");
            this.add("blocksplitting");
            this.add("blocksplittinglast");
            this.add("blocksplittingmax");
        }});

        public static final OptionsStruct of(Options options) {
            OptionsStruct struct = new OptionsStruct();
            struct.verbose = options.isVerbose() ? 1 : 0;
            struct.numiterations = options.getNumiterations();
            struct.blocksplitting = options.isBlocksplitting() ? 1 : 0;
            struct.blocksplittinglast = options.isBlocksplittinglast() ? 1 : 0;
            struct.blocksplittingmax = options.getBlocksplittingmax();

            return struct;
        }

        public int verbose;
        public int numiterations;
        public int blocksplitting;
        public int blocksplittinglast;
        public int blocksplittingmax;

        @Override
        protected List getFieldOrder() {
            return FIELD_ORDER;
        }
    }
}
