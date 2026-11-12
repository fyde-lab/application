package com.peltoche.fyde

/**
 * One account of a source: one installation of [script] for one login on
 * its site (design-system.md § Accounts of a source). A user can connect
 * the same source several times; each account has its own
 * [installationId], credentials ([parameters]), runs and status, and a
 * [label] telling it apart from the others. [shared] says whether [script]
 * has other accounts, which is when the label shows.
 */
data class SourceAccount(
    val installationId: String,
    val script: FydeScript,
    val label: String,
    val parameters: Map<String, FydeScriptParameterValue>,
    val shared: Boolean,
) {
    /** How snackbars, dialogs and the code sheet name it: "Ameli · Marie" once the source has several accounts, else "Ameli". */
    val name: String get() = if (shared) "${script.name}$ACCOUNT_SEPARATOR$label" else script.name
}

/** Between a source's name and its account's, in [SourceAccount.name]. */
internal const val ACCOUNT_SEPARATOR = " · "

/**
 * The accounts of [installations] whose script is among [scripts] (a script
 * no longer offered is left out), by source name then label, ignoring case.
 */
fun sourceAccountsOf(scripts: List<FydeScript>, installations: List<FydeInstallation>): List<SourceAccount> {
    val byId = scripts.associateBy { it.id }
    val perScript = installations.groupingBy { it.scriptId }.eachCount()
    return installations
        .mapNotNull { installation ->
            val script = byId[installation.scriptId] ?: return@mapNotNull null
            SourceAccount(
                installationId = installation.id,
                script = script,
                label = installation.label,
                parameters = installation.parameters,
                shared = (perScript[installation.scriptId] ?: 0) > 1,
            )
        }
        .sortedWith(compareBy({ it.script.name.lowercase() }, { it.label.lowercase() }))
}

/**
 * The label the SDK gives an account installed without one, as far as the
 * app can tell before it syncs back: the first non-blank value of [script]'s
 * non-secret text parameters, `username` first, then by name; else its name.
 * Only a placeholder for the moment between publishing and syncing: once
 * synced, the label is the SDK's own.
 */
internal fun provisionalLabel(script: FydeScript, parameters: Map<String, FydeScriptParameterValue>): String =
    script.parameters
        .filter { (_, parameter) -> !parameter.secret && parameter.type == FydeScriptParameterType.STRING }
        .keys
        .sortedWith(compareBy({ it != "username" }, { it }))
        .firstNotNullOfOrNull { key ->
            (parameters[key] as? FydeScriptParameterValue.Text)?.value?.trim()?.takeIf { it.isNotEmpty() }
        }
        ?: script.name
