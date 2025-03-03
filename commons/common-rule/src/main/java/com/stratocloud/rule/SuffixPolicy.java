package com.stratocloud.rule;

public record SuffixPolicy(SuffixType type, int suffixLength, int suffixStartNumber) {

    public static final SuffixPolicy NONE
            = new SuffixPolicy(SuffixType.NONE, 0, 0);

    public static final SuffixPolicy DEFAULT_RANDOM
            = new SuffixPolicy(SuffixType.RANDOM_STRING, 8, 0);
}
