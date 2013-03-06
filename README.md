jopfli `v0.0.1`
======

[Zopfli](https://code.google.com/p/zopfli/) native bindings for Java.

# Installation

**TODO**

# Usage 

```java
	import com.wotifgroup.zopfli.Jopfli

	// ...

	byte[] input = "Hello, world".getBytes(Charset.defaultCharset());

	// Gzip style.
	byte[] output = Jopfli.gzip(input), Options.SMALL_FILE_DEFAULTS);

	// Zlib style.
	byte[] output = Jopfli.zlib(input), Options.SMALL_FILE_DEFAULTS);

	// Raw DEFLATE style.
	byte[] output = Jopfli.deflate(input), Options.SMALL_FILE_DEFAULTS);

	// output will now contain Zopfli-flavoured deflate (with zlib/gzip headers)
```

# Configuring

Check out [Options](src/main/java/com/wotifgroup/zopfli/Options.java).

# TODO

 * Bundle Windows/OSX native libraries.
 * Investigate (Input|Output)Stream support.

# License

This project has been licensed under ASL 2.0. See the [LICENSE](LICENSE) file
for more information.