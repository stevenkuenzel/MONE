package util.random

import org.apache.commons.math3.random.MersenneTwister

/**
 * The random number generator. Based on the Mersenne Twister implementation of Apache.Commons.Math3.
 *
 */
class RandomProvider private constructor() : MersenneTwister() {

    companion object
    {
        private val defaultRandom = RandomProvider()

        /**
         * Initialize the seed-generating RNG with a seed.
         *
         * @param seed The seed.
         */
        fun initialize(seed : Long)
        {
            if (seed != 0L) {
                defaultRandom.setSeed(seed)
            }
        }

        /**
         * Create a new RNG based on the seed-generating RNG.
         *
         * @return
         */
        fun create() : RandomProvider
        {
            return RandomProvider(defaultRandom.nextLong())
        }
    }

    private constructor(seed : Long) : this()
    {
        setSeed(seed)
    }

//    override fun next(bits: Int): Int {
//
//        var numOfTries = 0
//
//        var next: Int? = null
//
//        while (next == null) {
//            next = try {
//                super.next(bits)
//            } catch (ex: java.lang.Exception) {
//                if (numOfTries++ >= 5)
//                {
//                    return 0
//                }
//
//                null
//            }
//        }
//
//        return next
//    }
}