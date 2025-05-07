package org.ysb33r.gradle.terraform.config

open class Json : ConfigExtension {
    override val name: String = "json"
    var enabled: Boolean = false

    /** Command-line parameter for JSON output.
     *
     */
    companion object {
        private const val JSON_FORMAT = "-json"
    }

    override fun getCommandLineArgs(): List<String> {
        return if (enabled) {
            listOf(JSON_FORMAT)
        } else {
            emptyList()
        }
    }
}