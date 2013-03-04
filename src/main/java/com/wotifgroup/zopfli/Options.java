package com.wotifgroup.zopfli;

public class Options {
    public static Options SMALL_FILE_DEFAULTS = new Options(15, true, false, 15);
    public static Options LARGE_FILE_DEFAULTS = new Options(5, true, false, 15);

    /* Whether to print output */
    private boolean verbose = false;

    /*
    Maximum amount of times to rerun forward and backward pass to optimize LZ77
    compression cost. Good values: 10, 15 for small files, 5 for files over
    several MB in size or it will be too slow.
    */
    private int numiterations = 15;

    /*
    If true, splits the data in multiple deflate blocks with optimal choice
    for the block boundaries. Block splitting gives better compression. Default:
    true (1).
    */
    private boolean blocksplitting = true;

    /*
    If true, chooses the optimal block split points only after doing the iterative
    LZ77 compression. If false, chooses the block split points first, then does
    iterative LZ77 on each individual block. Depending on the file, either first
    or last gives the best compression. Default: false (0).
    */
    private boolean blocksplittinglast = false;

    /*
    Maximum amount of blocks to split into (0 for unlimited, but this can give
    extreme results that hurt compression on some files). Default value: 15.
    */
    private int blocksplittingmax = 15;

    public Options(int numiterations, boolean blocksplitting, boolean blocksplittinglast, int blocksplittingmax) {
        this.numiterations = numiterations;
        this.blocksplitting = blocksplitting;
        this.blocksplittinglast = blocksplittinglast;
        this.blocksplittingmax = blocksplittingmax;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getNumiterations() {
        return numiterations;
    }

    public void setNumiterations(int numiterations) {
        this.numiterations = numiterations;
    }

    public boolean isBlocksplitting() {
        return blocksplitting;
    }

    public void setBlocksplitting(boolean blocksplitting) {
        this.blocksplitting = blocksplitting;
    }

    public boolean isBlocksplittinglast() {
        return blocksplittinglast;
    }

    public void setBlocksplittinglast(boolean blocksplittinglast) {
        this.blocksplittinglast = blocksplittinglast;
    }

    public int getBlocksplittingmax() {
        return blocksplittingmax;
    }

    public void setBlocksplittingmax(int blocksplittingmax) {
        this.blocksplittingmax = blocksplittingmax;
    }
}
