package com.softartdev.kvace.feature.agent.data

import com.softartdev.kvace.feature.agent.domain.ShellCommandRequest
import com.softartdev.kvace.feature.agent.domain.ShellCommandResult

internal object ShellCommandPolicy {
    private val allowedCommands = setOf("pwd", "ls", "cat", "head", "tail", "sed", "rg", "git")
    private val allowedGitSubcommands = setOf("status", "diff", "log", "show")
    private val rejectedGitOptions = setOf(
        "-c",
        "--exec-path",
        "--git-dir",
        "--work-tree",
        "--paginate",
        "--ext-diff",
        "--textconv",
        "--output",
        "--config-env",
    )
    private val blockedCharacters = setOf('|', ';', '&', '<', '>', '`')

    fun validate(request: ShellCommandRequest): ShellCommandResult.Rejected? {
        val command = request.command.trim()
        if (command.isBlank()) return ShellCommandResult.Rejected("Shell command is blank.")
        if (command != request.command || command in listOf(".", "..")) {
            return ShellCommandResult.Rejected("Shell command must be a single allowlisted executable name.")
        }
        if (command.containsPathSeparator() || command !in allowedCommands) {
            return ShellCommandResult.Rejected("Command \"$command\" is not in the read-only allowlist.")
        }
        val allTokens = listOf(command) + request.args + listOfNotNull(request.workingDirectory)
        allTokens.firstOrNull { it.hasBlockedShellSyntax() }?.let { token ->
            return ShellCommandResult.Rejected("Rejected unsafe shell token: $token")
        }
        request.args.firstOrNull { it.hasPathEscape() }?.let { token ->
            return ShellCommandResult.Rejected("Rejected path escape: $token")
        }
        request.args.firstOrNull { it.isAbsolutePathLike() }?.let { token ->
            return ShellCommandResult.Rejected("Absolute paths are not allowed in shell tool arguments: $token")
        }
        return when (command) {
            "git" -> validateGit(request.args)
            "sed" -> validateSed(request.args)
            "rg" -> validateRipgrep(request.args)
            "pwd" -> validatePwd(request.args)
            else -> null
        }
    }

    private fun validateGit(args: List<String>): ShellCommandResult.Rejected? {
        val subcommand = args.firstOrNull()
            ?: return ShellCommandResult.Rejected("git requires an allowlisted read-only subcommand.")
        if (subcommand !in allowedGitSubcommands) {
            return ShellCommandResult.Rejected("git $subcommand is not in the read-only allowlist.")
        }
        args.firstOrNull { option ->
            option in rejectedGitOptions || rejectedGitOptions.any { rejected -> option.startsWith("$rejected=") }
        }?.let { option ->
            return ShellCommandResult.Rejected("Rejected unsafe git option: $option")
        }
        return null
    }

    private fun validateSed(args: List<String>): ShellCommandResult.Rejected? {
        if (args.size < 2 || args.first() != "-n") {
            return ShellCommandResult.Rejected("sed is limited to `sed -n <range>p [file...]`.")
        }
        if (args.any { it == "-i" || it.startsWith("-i") }) {
            return ShellCommandResult.Rejected("sed in-place editing is not allowed.")
        }
        val script = args[1]
        if (!script.matches(Regex("""\d+(,\d+)?p"""))) {
            return ShellCommandResult.Rejected("sed is limited to numeric print ranges.")
        }
        args.drop(2).firstOrNull { it.startsWith("-") }?.let { option ->
            return ShellCommandResult.Rejected("sed only accepts file paths after the numeric print range: $option")
        }
        return null
    }

    private fun validateRipgrep(args: List<String>): ShellCommandResult.Rejected? =
        args.firstOrNull { it == "--pre" || it.startsWith("--pre=") || it == "--pre-glob" || it.startsWith("--pre-glob=") }
            ?.let { ShellCommandResult.Rejected("Rejected unsafe rg option: $it") }

    private fun validatePwd(args: List<String>): ShellCommandResult.Rejected? =
        args.takeIf { it.isNotEmpty() }?.let { ShellCommandResult.Rejected("pwd does not accept arguments.") }

    private fun String.hasBlockedShellSyntax(): Boolean =
        any { it in blockedCharacters || it == '\n' || it == '\r' } || contains("\$(")

    private fun String.containsPathSeparator(): Boolean = contains("/") || contains("\\")

    private fun String.hasPathEscape(): Boolean =
        contains("..")

    private fun String.isAbsolutePathLike(): Boolean =
        startsWith("/") || startsWith("\\") || startsWith("~") || matches(Regex("""[A-Za-z]:[\\/].*"""))
}
