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

import com.google.common.io.Files;
import com.ochafik.lang.jnaerator.runtime.CharByReference;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Jopfli {
    private static final ZopfliLibrary LIB;

    /**
     * TODO: This all feels very hacky. Should be using something like OneJAR or some kind of library that abstracts
     * this nonsense away.
     */
    static {
        ClassLoader cl = Jopfli.class.getClassLoader();
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        String libName = "";
        if(osName.equals("Linux")) {
            libName = "libzopfli.so";
        }

        URL url = cl.getResource(String.format("native/%s/%s/%s", osName, osArch, libName));

        if(url == null) {
            throw new UnsatisfiedLinkError("No zopfli support for " + osName + " " + osArch + ". File an issue!");
        }

        try {
            File nativeDir = Files.createTempDir();
            File tempLib = new File(nativeDir, libName);
            tempLib.deleteOnExit();
            nativeDir.deleteOnExit();

            // TODO: apache ioutils or something here?
            FileOutputStream tempOut = new FileOutputStream(tempLib);
            byte[] buf = new byte[4096];
            int r;
            InputStream in = url.openStream();
            while((r = in.read(buf, 0, buf.length)) > -1) {
                tempOut.write(buf, 0, r);
            }
            tempOut.close();

            NativeLibrary.addSearchPath("zopfli", nativeDir.getPath());
        }
        catch(IOException ioe) {
            throw new RuntimeException("Failed to load zopfli native library", ioe);
        }

        LIB = (ZopfliLibrary) Native.loadLibrary("zopfli", ZopfliLibrary.class);
    }

    private static CharByReference BP = new CharByReference((char)0);

    /**
     *  Zopfli compress the data, and then return the raw DEFLATE stream.
     * @param data
     * @param opts
     * @return
     */
    public static final byte[] deflate(byte[] data, Options opts) {
        NativeSizeByReference outSize = new NativeSizeByReference();
        PointerByReference out =  new PointerByReference(new Pointer(0));
        LIB.Deflate(ZopfliLibrary.OptionsStruct.of(opts), 2, 1, data, new NativeSize(data.length), BP, out, outSize);
        return out.getValue().getByteArray(0, outSize.getValue().intValue());
    }

    /**
     * Zopfli compress the data, and then return a zlib-compatible stream.
     * @param data
     * @param opts
     * @return
     */
    public static final byte[] zlib(byte[] data, Options opts) {
        NativeSizeByReference outSize = new NativeSizeByReference();
        PointerByReference out =  new PointerByReference(new Pointer(0));
        LIB.ZlibCompress(ZopfliLibrary.OptionsStruct.of(opts), data, new NativeSize(data.length), out, outSize);
        return out.getValue().getByteArray(0, outSize.getValue().intValue());
    }

    /**
     * Zopfli compress the data, and then return a gzip-compatible stream.
     * @param data
     * @param opts
     * @return
     */
    public static final byte[] gzip(byte[] data, Options opts) {
        NativeSizeByReference outSize = new NativeSizeByReference();
        PointerByReference out =  new PointerByReference(new Pointer(0));
        LIB.GzipCompress(ZopfliLibrary.OptionsStruct.of(opts), data, new NativeSize(data.length), out, outSize);
        return out.getValue().getByteArray(0, outSize.getValue().intValue());
    }
}
