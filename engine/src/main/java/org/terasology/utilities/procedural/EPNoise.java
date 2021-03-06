/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.utilities.procedural;

import org.terasology.math.TeraMath;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;

/**
 * Edited Perlin noise generator based on the reference implementation by Ken Perlin.
 * and on implementation by Benjamin Glatzel
 *
 * @author Esa-Petri Tirkkonen <esereja@yahoo.co.uk>
 */
public class EPNoise implements Noise {

    private static final double LACUNARITY = 2.1379201;
    private static final double H = 0.836281;
    private static final double TAN_BYTE = Math.atan(256);

    private double[] spectralWeights;

    private final int[] noisePermutations = new int[512];
    private boolean recomputeSpectralWeights = true;
    private int octaves = 9;
    private boolean on;
    //static final boolean verbose = false;

    /**
     * Init. a new generator with a given seed value.
     *
     * @param seed   The seed value
     */
    public EPNoise(int seed) {
        this(seed, NoiseType.RANDOM);
    }

    /**
     * Init. a new generator with a given seed value.
     *
     * @param seed   The seed value
     * @param type   Type of generator
     */
    public EPNoise(int seed, NoiseType type) {
        this(seed, type, false);
    }

    /**
     * Init. a new generator with a given seed value.
     *
     * @param seed   The seed value
     * @param type   Type of generator
     * @param random allow extra randomization whit mostly odd results
     */
    public EPNoise(int seed, NoiseType type, boolean random) {
        FastRandom rand = new FastRandom(seed);
        on = type.isUsingFBM();
        int[] noiseTable = new int[256];

        // Init. the noise table
        for (int i = 0; i < 256; i++) {
            if (random) {
                noiseTable[i] = rand.nextInt(256);
            } else {
                noiseTable[i] = i;
            }
        }

        // Shuffle the array
        for (int i = 0; i < 256; i++) {
            int j = type.generate(rand);
            if (j < 0) {
                j = 0;
            } else if (j > 255) {
                j = 255;
            }

            if (type != NoiseType.NONE) {
                int swap = noiseTable[i];
                noiseTable[i] = noiseTable[j];
                noiseTable[j] = swap;
            }
        }

        // Finally replicate the noise permutations in the remaining 256 index
        // positions
        for (int i = 0; i < 256; i++) {
            noisePermutations[i + 256] = noiseTable[i];
            noisePermutations[i] = noiseTable[i];
        }
    }

    /**
     * Returns the noise value at the given position.
     *
     * @param posX Position on the x-axis
     * @param posY Position on the y-axis
     * @param posZ Position on the z-axis
     * @return The noise value
     */
    public double noise(double posX, double posY, double posZ) {
        int xInt = (int) TeraMath.fastFloor(posX) & 255;
        int yInt = (int) TeraMath.fastFloor(posY) & 255;
        int zInt = (int) TeraMath.fastFloor(posZ) & 255;

        double x = posX - TeraMath.fastFloor(posX);
        double y = posY - TeraMath.fastFloor(posY);
        double z = posZ - TeraMath.fastFloor(posZ);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        int a = noisePermutations[xInt] + yInt;
        int aa = noisePermutations[a] + zInt;
        int ab = noisePermutations[(a + 1)] + zInt;
        int b = noisePermutations[(xInt + 1)] + yInt;
        int ba = noisePermutations[b] + zInt;
        int bb = noisePermutations[(b + 1)] + zInt;

        return lerp(
                w,
                lerp(v,
                        lerp(u, grad(noisePermutations[aa], x, y, z),
                                grad(noisePermutations[ba], x - 1, y, z)),
                        lerp(u, grad(noisePermutations[ab], x, y - 1, z),
                                grad(noisePermutations[bb], x - 1, y - 1, z))),
                lerp(v,
                        lerp(u,
                                grad(noisePermutations[(aa + 1)], x, y, z - 1),
                                grad(noisePermutations[(ba + 1)], x - 1, y,
                                        z - 1)),
                        lerp(u,
                                grad(noisePermutations[(ab + 1)], x, y - 1,
                                        z - 1),
                                grad(noisePermutations[(bb + 1)], x - 1,
                                        y - 1, z - 1))));
    }

    /**
     * Returns Fractional Brownian Motion at the given position.
     *
     * @param posX Position on the x-axis
     * @param posY Position on the y-axis
     * @param posZ Position on the z-axis
     * @return The noise value
     */
    public double fBm(double posX, double posY, double posZ) {
        double result = 0.0;

        double x = posX;
        double y = posY;
        double z = posZ;

        if (on) {
            if (recomputeSpectralWeights) {
                spectralWeights = new double[octaves];

                for (int i = 0; i < octaves; i++) {
                    spectralWeights[i] = java.lang.Math.pow(LACUNARITY, -H * i);
                }

                recomputeSpectralWeights = false;
            }

            for (int i = 0; i < octaves; i++) {
                result += noise(x, y, z) * spectralWeights[i];

                x *= LACUNARITY;
                y *= LACUNARITY;
                z *= LACUNARITY;
            }
        }
        return result;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public void setOctaves(int octaves) {
        this.octaves = octaves;
        recomputeSpectralWeights = true;
    }

    public int getOctaves() {
        return octaves;
    }

    public enum NoiseType {
        // Default
        RANDOM(true) {
            @Override
            public int generate(Random rand) {
                return rand.nextInt(256);
            }
        },

        // Smoother
        SINE(true) {
            @Override
            public int generate(Random rand) {
                return (int) (Math.sin(rand.nextDouble() * Math.PI) * 256.0);
            }
        },

        TANGENT(true) {
            @Override
            public int generate(Random rand) {
                // TAN_BYTE makes Math.tan return a value in range [0, 256)
                return (int) Math.tan(rand.nextDouble() * TAN_BYTE);
            }
        },

        // Lots of low values
        HYPERBOLIC_SINE(true) {
            @Override
            public int generate(Random rand) {
                // 6.238328 is aprox. asinh(256), so sinh will return a value in range [0, 256)
                return (int) Math.sinh(rand.nextDouble() * 6.238328);
            }
        },

        HYPERBOLIC_TANGENT(true) {
            @Override
            public int generate(Random rand) {
                // TODO: Remove magic
                int result = (int) TeraMath
                        .fastFloor((Math.tanh(rand.nextDouble() % 3) / Math
                                .tanh(3)) * 256);
                return (result < 0) ? -result : result;
            }
        },

        // High elevation and flat top
        LOGARYTHM(true) {
            @Override
            public int generate(Random rand) {
                // TODO: Remove magic
                int result = (int) (TeraMath
                        .fastFloor((Math.log(rand.nextDouble() % 4) / 4) * 256)) % 256;
                return (result < 0) ? -result : result;
            }
        },

        // Lot of mid values
        ARCSINE(true) {
            @Override
            public int generate(Random rand) {
                return (int) (Math.acos(rand.nextDouble()) * 256.0 / Math.PI);
            }
        },

        // Types below are for debugging purposes only

        // Only zeroes, causes layer to disappear
        ZEROES(false) {
            @Override
            public int generate(Random rand) {
                return 0;
            }
        },

        NONE(true) {
            @Override
            public int generate(Random rand) {
                return 0;
            }
        };

        private boolean usingFBM;

        private NoiseType(boolean usingFBM) {
            this.usingFBM = usingFBM;
        }

        public abstract int generate(Random rand);

        public boolean isUsingFBM() {
            return usingFBM;
        }
    }
}
