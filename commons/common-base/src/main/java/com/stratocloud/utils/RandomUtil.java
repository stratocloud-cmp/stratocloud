package com.stratocloud.utils;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomUtil {
    private static final String characters = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final String capitalLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String specialCharacters = "!@$_-+=";


    private static String generateRandomString(String charCandidates, int length){
        Random random = new Random();
        return random.ints(length, 0, charCandidates.length())
                .mapToObj(charCandidates::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    public static String generatePasswordLen12(){
        String s1 = generateRandomString(characters, 8);
        String s2 = generateRandomString(specialCharacters, 1);
        String s3 = generateRandomString(capitalLetters, 3);
        return s1+s2+s3;
    }

    public static String generateRandomString(int length) {
        return generateRandomString(characters, length);
    }

    public static int generateRandomInteger(int origin, int bound){
        Random random = new Random();
        return random.nextInt(origin, bound);
    }

    public static int generateRandomInteger(int bound){
        Random random = new Random();
        return random.nextInt(bound);
    }

    public static boolean randomBoolean(float probability) {
        Assert.isTrue(
                probability >= 0.0f && probability <= 1.0f,
                "Invalid probability: %s.".formatted(probability)
        );
        Random random = new Random();

        float nextFloat = random.nextFloat(1.0f);

        return nextFloat <= probability;
    }

    public static <E> Optional<E> selectRandomly(List<E> candidates){
        if(Utils.isEmpty(candidates))
            return Optional.empty();

        int randomIndex = generateRandomInteger(candidates.size());
        return Optional.of(candidates.get(randomIndex));
    }
}
