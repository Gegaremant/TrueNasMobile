package com.gegaremant.truenasmobile.data.helpers

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.gegaremant.truenasmobile.R
import com.gegaremant.truenasmobile.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(@StringRes val displayNameRes: Int) {
    LIGHT(R.string.theme_mode_light),
    DARK(R.string.theme_mode_dark),
    SYSTEM(R.string.theme_mode_system)
}

/**
 * Per-user personalization storage (theme, black mode, compact nav, navbar layout).
 *
 * All personalization is scoped to the currently-active user account so that one
 * user's choices never leak into another user's session. Settings fall back to
 * [defaults] for a user who has never customized anything.
 *
 * To add a future personalization setting, add a field to [PersonalizationState],
 * a serialized key in [_key], and a read/write in [loadForUser]/[saveForUser].
 *
 * The active account is owned by `MainViewModel.setActiveUser` and reaches this
 * manager as an explicit key - there is deliberately no "guess the current user
 * from disk" helper. An earlier `currentUserKey(context)` claimed to resolve the
 * last-used profile but always returned [DEFAULT_USER_KEY], which would have
 * leaked one account's theme and navbar into another's.
 */
data class PersonalizationState(
    val theme: AppTheme = AppTheme.TRUENASMOBILE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val blackMode: Boolean = false,
    val compactNav: Boolean = false,
    val navbarDestinations: List<NavbarDestination> = NavbarDestination.defaults,
    val searchBarBottom: Boolean = false
)

object PersonalizationManager {

    // Reactive state so UI (bottom nav, theme) can update without a restart.
    private val _state = MutableStateFlow(PersonalizationState())
    val state: StateFlow<PersonalizationState> = _state.asStateFlow()

    private const val PREFS_NAME = "truenasmobile_personalization"

    // Default user key used when no account is active yet.
    const val DEFAULT_USER_KEY = "default"

    /** Loads personalization for [userKey] and updates the reactive state. */
    fun loadForUser(context: Context, userKey: String) {
        currentActiveKey = userKey
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val theme = runCatching {
            AppTheme.valueOf(prefs.getString(key(userKey, "theme"), AppTheme.TRUENASMOBILE.name) ?: AppTheme.TRUENASMOBILE.name)
        }.getOrDefault(AppTheme.TRUENASMOBILE)

        val themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(key(userKey, "theme_mode"), ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

        val blackMode = prefs.getBoolean(key(userKey, "black_mode"), false)
        val compactNav = prefs.getBoolean(key(userKey, "compact_nav"), false)
        val searchBarBottom = prefs.getBoolean(key(userKey, "search_bar_bottom"), false)

        val navbar = loadNavbar(prefs, userKey)

        _state.value = PersonalizationState(
            theme = theme,
            themeMode = themeMode,
            blackMode = blackMode,
            compactNav = compactNav,
            navbarDestinations = navbar,
            searchBarBottom = searchBarBottom
        )
    }

    fun saveTheme(context: Context, userKey: String, theme: AppTheme) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(key(userKey, "theme"), theme.name) }
        _state.value = _state.value.copy(theme = theme)
    }

    fun saveThemeMode(context: Context, userKey: String, themeMode: ThemeMode) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(key(userKey, "theme_mode"), themeMode.name) }
        _state.value = _state.value.copy(themeMode = themeMode)
    }

    fun saveBlackMode(context: Context, userKey: String, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(key(userKey, "black_mode"), enabled) }
        _state.value = _state.value.copy(blackMode = enabled)
    }

    fun saveCompactNav(context: Context, userKey: String, compact: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(key(userKey, "compact_nav"), compact) }
        _state.value = _state.value.copy(compactNav = compact)
    }

    fun saveNavbar(context: Context, userKey: String, destinations: List<NavbarDestination>) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(key(userKey, "navbar"), destinations.joinToString(",") { it.name })
            }
        _state.value = _state.value.copy(navbarDestinations = effectiveDestinations(destinations))
    }

    fun saveSearchBarBottom(context: Context, userKey: String, bottom: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(key(userKey, "search_bar_bottom"), bottom) }
        _state.value = _state.value.copy(searchBarBottom = bottom)
    }

    /** Removes all personalization for a user (called when an account is deleted). */
    fun deleteForUser(context: Context, userKey: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith("${userKey}_") }
            .forEach { editor.remove(it) }
        editor.apply()

        // If the deleted user is the active one, reset to defaults.
        if (userKey == currentActiveKey) {
            _state.value = PersonalizationState()
        }
    }

    @Volatile
    private var currentActiveKey: String = DEFAULT_USER_KEY

    /** Tracks which user the reactive state currently reflects (set from loadForUser). */
    fun getActiveKey(): String = currentActiveKey

    /** All destinations a user could opt into, in a sensible default order. */
    val availableDestinations: List<NavbarDestination>
        get() = NavbarDestination.entries

    /**
     * Resolve the effective, ordered list of bar destinations.
     *
     * Guarantees, in order of importance:
     *  - [NavbarDestination.HOME] is always present and always first;
     *  - the user's own order is preserved (this used to be silently discarded,
     *    which made reordering in the UI do nothing);
     *  - duplicates are collapsed;
     *  - an empty or all-optional selection still yields a usable bar (Home).
     */
    fun effectiveDestinations(selected: List<NavbarDestination>): List<NavbarDestination> {
        val ordered = LinkedHashSet<NavbarDestination>()
        ordered.add(NavbarDestination.HOME)
        selected.forEach { dest ->
            if (dest != NavbarDestination.HOME) ordered.add(dest)
        }
        return ordered.toList()
    }

    /**
     * Toggles one optional destination on or off in [current], keeping order.
     * Used by the bottom-navigation editor: switching something off forgets its
     * position, switching it back on appends it to the end.
     */
    fun toggleDestination(
        current: List<NavbarDestination>,
        destination: NavbarDestination
    ): List<NavbarDestination> {
        if (destination.isRequired) return effectiveDestinations(current)
        val next = current.toMutableList()
        if (!next.remove(destination)) next.add(destination)
        return effectiveDestinations(next)
    }

    /**
     * Moves [destination] one step towards the front ([delta] -1) or the back
     * (+1). Returns the list unchanged when the move is not possible, so the
     * caller can disable the arrow instead of guessing.
     */
    fun moveDestination(
        current: List<NavbarDestination>,
        destination: NavbarDestination,
        delta: Int
    ): List<NavbarDestination> {
        val next = effectiveDestinations(current).toMutableList()
        val index = next.indexOf(destination)
        val target = index + delta
        if (index <= 0 || target < 1 || target >= next.size) return next
        val item = next.removeAt(index)
        next.add(target, item)
        return next
    }

    private fun loadNavbar(
        prefs: android.content.SharedPreferences,
        userKey: String
    ): List<NavbarDestination> {
        val raw = prefs.getString(key(userKey, "navbar"), null)
        if (raw.isNullOrBlank()) {
            return NavbarDestination.defaults
        }
        val parsed = raw.split(",").mapNotNull { name ->
            runCatching { NavbarDestination.valueOf(name.trim()) }.getOrNull()
        }
        return effectiveDestinations(parsed)
    }

    private fun key(userKey: String, field: String) = "${userKey}_${field}"
}
