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

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

import static org.fest.assertions.api.Assertions.assertThat;

public class JopfliDeflaterTests {
    @Test
    public void testBasicDeflate() {
        JopfliDeflater deflater = new JopfliDeflater();

        // Fresh deflater should be expecting input.
        assertThat(deflater.needsInput()).isTrue();

        deflater.setInput("Hello, World!".getBytes(Charsets.UTF_8));

        // Default window size is huge (20mb), so should still definitely be demanding more input.
        assertThat(deflater.needsInput()).isTrue();

        deflater.finish();

        // Finishing up, so shouldn't be expecting any more input.
        assertThat(deflater.needsInput()).isFalse();

        // Haven't actually deflated and output what we provided though, so shouldn't actually be finished yet.
        assertThat(deflater.finished()).isFalse();

        byte[] out = new byte[1024];
        deflater.deflate(out);

        // Now we should be done.
        assertThat(deflater.needsInput()).isFalse();
        assertThat(deflater.finished()).isTrue();

        assertThat(new String(inflate(out), Charsets.UTF_8)).isEqualTo("Hello, World!");
    }

    private static final byte[] inflate(byte[] data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InflaterOutputStream inflaterOut = new InflaterOutputStream(baos);
            inflaterOut.write(data);
            inflaterOut.close();
            return baos.toByteArray();
        }
        catch(Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
