package test

import org.apache.commons.cli.*

object CommandLineSupport {
    private const val EFAULT_REPEAT_TIMES = 100
    private const val EFAULT_SLEEP_AFTER_EXCEPTION_MS = 100L

    fun createOptions(): Options {
        val options = Options()

        options.addOption(Option("h", "print this message"))
        options.addOption(Option("v", "verbose"))

        val repeatTime = Option.builder("rt")
            .argName("repeatTime")
            .longOpt("repeatTime")
            .hasArg()
            .numberOfArgs(1)
            .type(Long::class.java)
            .desc("Repeat time (ms). If account has been changed that sleep. Default value is 100 ms")
            .build()
        options.addOption(repeatTime)

        val repeatTimes = Option.builder("rts")
            .argName("repeatTimes")
            .longOpt("repeatTimes")
            .hasArg()
            .numberOfArgs(1)
            .type(Int::class.java)
            .desc("Repeat times. If account has been changed that repeat. Default value is 100")
            .build()
        options.addOption(repeatTimes)

        return options
    }

    fun help() {
        val formatter = HelpFormatter()
        formatter.printHelp("Money transfer application", "", createOptions(), "", true)
    }

    @Throws(ParseException::class)
    fun getRepeatTime(line: CommandLine): Long {
        if (!line.hasOption("rt")) {
            return EFAULT_SLEEP_AFTER_EXCEPTION_MS
        }
        val parameter = line.getOptionValue("rt")
        val converted = parameter.toLongOrNull()
            ?: throw IllegalArgumentException("Wrong parameter repeatTime = $parameter. It should be Int")
        require(converted != 0L) { "Wrong parameter!" }
        return converted
    }

    @Throws(ParseException::class)
    fun getRepeatTimes(line: CommandLine): Int {
        if (!line.hasOption("rts")) {
            return EFAULT_REPEAT_TIMES
        }
        val parameter = line.getOptionValue("rts")
        return parameter.toIntOrNull()
            ?: throw IllegalArgumentException("Wrong parameter repeatTimes = $parameter. It should be Int")
    }

    fun getVerbose(line: CommandLine): Boolean {
        return line.hasOption("v")
    }
}
