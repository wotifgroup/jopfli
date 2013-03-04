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
import com.sun.jna.Library;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JNA wrapping for the stuff in Zopfli native that we actually want to invoke.
 * @author Sam Day
 */
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
